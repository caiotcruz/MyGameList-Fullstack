import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { MessageService, ConversationSummary, Message, MessageableContact } from '../../services/message';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit, OnDestroy {
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

  ngOnInit() {
    this.carregarLista();

    this.route.paramMap.subscribe(params => {
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
    this.activeUserId = userId;
    this.activeUserName = name || this.findNameFallback(userId) || 'Carregando...';
    this.activeUserAvatar = avatar ?? this.findAvatarFallback(userId);
    this.isLoadingThread = true;

    if (this.router.url !== `/messages/${userId}`) {
      this.router.navigate(['/messages', userId]);
    }

    this.messageService.getThread(userId).subscribe({
      next: (res) => {
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
      error: () => {
        this.isLoadingThread = false;
        this.cdr.detectChanges();
      }
    });
  }

  atualizarThreadSilencioso(userId: number) {
    this.messageService.getThread(userId).subscribe({
      next: (res) => {
        if (res.messages.length !== this.thread.length) {
          const foiNoFinal = this.isScrolledToBottom();
          this.thread = res.messages;
          if (foiNoFinal) {
            this.scrollToBottom();
          }
          this.cdr.detectChanges();
        }
      }
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

  enviar() {
    if (!this.draft.trim() || !this.activeUserId || this.isSending) return;

    const content = this.draft.trim();
    this.isSending = true;

    this.messageService.sendMessage(this.activeUserId, content).subscribe({
      next: (msg) => {
        this.thread = [...this.thread, msg];
        this.draft = '';
        this.isSending = false;
        this.scrollToBottom();
        this.carregarLista(true);
        this.cdr.detectChanges();
      },
      error: () => {
        this.isSending = false;
        alert('Erro ao enviar mensagem.');
        this.cdr.detectChanges();
      }
    });
  }

  fecharThread() {
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