import { TEXT_LIMITS } from '../../shared/text-limits';
import { Subject, takeUntil } from 'rxjs';
import { SafetyActions } from '../../components/safety-actions/safety-actions';
import { Feedback, errorMessage } from '../../services/feedback';
import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { MessageService, ConversationSummary, Message, MessageableContact } from '../../services/message';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [SafetyActions, CommonModule, FormsModule, RouterModule],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit, OnDestroy {
  readonly limits = TEXT_LIMITS;

  feedback=inject(Feedback);

  messageService = inject(MessageService);
  route = inject(ActivatedRoute);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  @ViewChild('threadScroll') threadScroll?: ElementRef<HTMLDivElement>;

  conversations: ConversationSummary[] = [];
  contacts: MessageableContact[] = [];
  isLoadingList = true;

  activeUserId: number | null = null;
  activeUserName = '';
  activeUserAvatar: string | null = null;
  thread: Message[] = [];
  isLoadingThread = false;

  draft = '';
  isSending = false;

  private pollInterval: any;
  private destroy$=new Subject<void>();
  canSend=false;threadError="";
  private drafts=new Map<number,string>();
  onBlocked(){this.thread=[];this.fecharThread();this.carregarLista();}

  ngOnInit() {
    this.carregarLista();

    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const userIdParam = params.get('userId');
      if (userIdParam) {
        const targetId = Number(userIdParam);
        if (targetId !== this.activeUserId) {
          this.abrirConversa(targetId, null, null);
        }
      } else {
        this.activeUserId = null;
        this.thread = [];
      }
    });

    this.pollInterval = setInterval(() => {
      this.carregarLista(true);
      if (this.activeUserId) {
        this.atualizarThreadSilencioso(this.activeUserId);
      }
    }, 5000);
  }

  ngOnDestroy() {
    this.destroy$.next();this.destroy$.complete();
    if (this.pollInterval) clearInterval(this.pollInterval);
  }

  carregarLista(silent = false) {
    if (!silent) this.isLoadingList = true;

    this.messageService.getConversations().subscribe({
      next: (res) => {
        this.conversations = res;
        this.isLoadingList = false;
        this.carregarContatos();
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingList = false;
        this.cdr.detectChanges();
      }
    });
  }

  carregarContatos() {
    this.messageService.getContacts().subscribe({
      next: (res) => {
        this.contacts = res;
        this.cdr.detectChanges();
      }
    });
  }

  abrirConversa(userId: number, name: string | null, avatar: string | null) {
    if(this.activeUserId)this.drafts.set(this.activeUserId,this.draft);
    this.draft=this.drafts.get(userId)||"";
    this.thread=[];this.threadError="";this.canSend=false;
    this.activeUserId = userId;
    this.activeUserName = name || this.findNameFallback(userId) || 'Carregando...';
    this.activeUserAvatar = avatar ?? this.findAvatarFallback(userId);
    this.isLoadingThread = true;

    if (this.router.url !== `/messages/${userId}`) {
      this.router.navigate(['/messages', userId]);
    }

    this.messageService.getThread(userId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        if(userId!==this.activeUserId)return;
        this.canSend=res.canSend;
        if (res.partner) {
          this.activeUserName = res.partner.name;
          this.activeUserAvatar = res.partner.profilePicture;
        }

        this.thread = res.messages;
        this.isLoadingThread = false;
        this.scrollToBottom();
        this.carregarLista(true);
        this.cdr.detectChanges();
      },
      error: (err) => {
        if(userId!==this.activeUserId)return;
        this.threadError=errorMessage(err);this.thread=[];this.canSend=false;
        this.isLoadingThread = false;
        this.cdr.detectChanges();
      }
    });
  }

  atualizarThreadSilencioso(userId: number) {
    this.messageService.getThread(userId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        if(userId!==this.activeUserId)return;
        this.canSend=res.canSend;
        if (res.messages.length !== this.thread.length) {
          const foiNoFinal = this.isScrolledToBottom();
          this.thread = res.messages;
          if (foiNoFinal) {
            this.scrollToBottom();
          }
          this.cdr.detectChanges();
        }
      },
      error:err=>{if(userId!==this.activeUserId)return;this.canSend=false;if(err.status===403){this.thread=[];this.threadError=errorMessage(err);}this.cdr.detectChanges();}
    });
  }

  private findNameFallback(userId: number): string {
    const fromConv = this.conversations.find(c => c.otherUser.id === userId);
    if (fromConv) return fromConv.otherUser.name;
    const fromContact = this.contacts.find(c => c.id === userId);
    return fromContact?.name || '';
  }

  private findAvatarFallback(userId: number): string | null {
    const fromConv = this.conversations.find(c => c.otherUser.id === userId);
    if (fromConv) return fromConv.otherUser.profilePicture;
    const fromContact = this.contacts.find(c => c.id === userId);
    return fromContact?.profilePicture || null;
  }

  onMessageEnter(event: Event) {
    const key=event as KeyboardEvent;
    if (!key.shiftKey && !key.isComposing) { key.preventDefault(); this.enviar(); }
  }

  enviar() {
    if(this.draft.length>this.limits.MESSAGE){this.feedback.show('Mensagem acima do limite. Seu texto foi preservado.');return;}
    if(!this.canSend)return;
    if (!this.draft.trim() || !this.activeUserId || this.isSending) return;

    const content = this.draft.trim();
    const recipient=this.activeUserId;
    this.isSending = true;

    this.messageService.sendMessage(this.activeUserId, content).subscribe({
      next: (msg) => {
        this.drafts.delete(recipient);
        if(recipient!==this.activeUserId){this.isSending=false;return;}
        this.thread = [...this.thread, msg];
        this.draft = '';
        this.isSending = false;
        this.scrollToBottom();
        this.carregarLista(true);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isSending = false;
        this.feedback.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  fecharThread() {
    if(this.activeUserId)this.drafts.set(this.activeUserId,this.draft);
    this.draft="";this.canSend=false;this.threadError="";
    this.activeUserId = null;
    this.thread = [];
    this.router.navigate(['/messages']);
  }

  private isScrolledToBottom(): boolean {
    if (!this.threadScroll) return true;
    const el = this.threadScroll.nativeElement;
    return el.scrollHeight - el.scrollTop <= el.clientHeight + 50;
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.threadScroll) {
        this.threadScroll.nativeElement.scrollTop = this.threadScroll.nativeElement.scrollHeight;
      }
    }, 50);
  }

  trackByMessageId(index: number, item: Message) {
    return item.id;
  }
}
