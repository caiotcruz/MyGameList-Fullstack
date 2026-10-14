import { TEXT_LIMITS } from '../../shared/text-limits';
import { SafetyActions } from '../../components/safety-actions/safety-actions';
import { Feedback, errorMessage } from '../../services/feedback';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { DatePipe, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivityService } from '../../services/activity';
import { CommunityService } from '../../services/community';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [SafetyActions, CommonModule, DatePipe, FormsModule, RouterModule],
  templateUrl: './feed.html',
  styleUrl: './feed.css'
})
export class Feed implements OnInit {
  readonly limits = TEXT_LIMITS;

  feedback=inject(Feedback);

  activityService = inject(ActivityService);
  communityService = inject(CommunityService);
  cdr = inject(ChangeDetectorRef);

  activities: any[] = [];
  myId: number = 0;

  nextCursor: number | null = null;
  hasMore = true;
  loading = false;
  feedError = false;

  ngOnInit() {
    this.myId = Number(localStorage.getItem('userId'));
    this.loadMore();
  }

  loadMore() {
    if (this.loading || !this.hasMore) return;
    this.loading = true;
    this.feedError = false;
    this.activityService.getFeed(this.nextCursor).subscribe({
      next: data => {
        const seen = new Set(this.activities.map(a => a.id));
        this.activities.push(...data.items.filter(a => !seen.has(a.id)).map(a => ({
          ...a, showComments: false, showSpoilerText: false
        })));
        this.hasMore = data.hasMore;
        this.nextCursor = data.nextCursor;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.feedError = true; this.cdr.detectChanges(); }
    });
  }

  toggleSpoiler(item: any) {
    item.showSpoilerText = !item.showSpoilerText;
    this.cdr.detectChanges();
  }

  toggleLike(item: any) {
    item.likedByMe = !item.likedByMe;
    item.likesCount += item.likedByMe ? 1 : -1;

    this.communityService.toggleLike(item.id).subscribe({
      error: () => {
        item.likedByMe = !item.likedByMe;
        item.likesCount += item.likedByMe ? 1 : -1;
        this.feedback.show('Erro ao curtir');
      }
    });
  }

  toggleComments(item: any) {
    item.showComments = !item.showComments;
  }

  enviarComentario(item: any, inputHtml: HTMLInputElement | HTMLTextAreaElement) {
    const texto = inputHtml.value;
    if (!texto.trim()) return;
    if(texto.length>this.limits.COMMENT){this.feedback.show('Comentário acima do limite. Seu texto foi preservado.');return;}

    this.communityService.postComment(item.id, texto).subscribe({
      next: (novoComentario) => {
        item.comments = [...(item.comments || []), novoComentario];
        inputHtml.value = '';
        this.cdr.detectChanges();
      },
      error: () => this.feedback.show('Erro ao comentar')
    });
  }

  podeDeletarComentario(comment: any, activityUser: any): boolean {
    if (!this.myId) return false;
    return comment.user?.id === this.myId || activityUser?.id === this.myId;
  }

  deletarComentario(item: any, commentId: number) {
    const backupComments = [...(item.comments || [])];
    item.comments = item.comments.filter((c: any) => c.id !== commentId);
    this.cdr.detectChanges();

    this.communityService.deleteComment(item.id, commentId).subscribe({
      error: () => {
        item.comments = backupComments;
        this.cdr.detectChanges();
        this.feedback.show('Erro ao deletar comentário.');
      }
    });
  }

  hasAction(item: any, type: string): boolean {
    return item.types?.includes(type);
  }

  getRatingValue(item: any): number | null {
    const score = item.ratingValue ?? item.score ?? item.ratingScore;

    if (score !== undefined && score !== null && Number(score) > 0) {
      return Number(score);
    }
    return null;
  }

  temNota(item: any): boolean {
    return this.getRatingValue(item) !== null;
  }

  getActionText(type: string): string {
    switch(type) {
      case 'ADDED_TO_LIST': return 'adicionou à biblioteca';
      case 'CHANGED_STATUS': return 'alterou o status para';
      case 'RATED': return 'deu nota';
      case 'REVIEWED': return 'fez uma review de';
      default: return 'atualizou';
    }
  }

  formatStatus(status: string): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase();
  }
}
