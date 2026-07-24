import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { DatePipe, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { ActivityService } from '../../services/activity'; 
import { CommunityService } from '../../services/community'; 
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, RouterModule], 
  templateUrl: './feed.html',
  styleUrl: './feed.css'
})
export class Feed implements OnInit {
  activityService = inject(ActivityService);
  communityService = inject(CommunityService);
  cdr = inject(ChangeDetectorRef); 

  activities: any[] = []; 
  myId: number = 0;

  ngOnInit() {
    this.myId = Number(localStorage.getItem('userId'));
    this.activityService.getFeed().subscribe({
      next: (data) => {
        this.activities = data.map((a: any) => ({ ...a, showComments: false }));
        this.cdr.detectChanges(); 
      }
    });
  }

  toggleLike(item: any) {
    item.likedByMe = !item.likedByMe;
    item.likesCount += item.likedByMe ? 1 : -1;

    this.communityService.toggleLike(item.id).subscribe({
      error: () => {
        item.likedByMe = !item.likedByMe;
        item.likesCount += item.likedByMe ? 1 : -1;
        alert('Erro ao curtir');
      }
    });
  }

  toggleComments(item: any) {
    item.showComments = !item.showComments;
  }

  enviarComentario(item: any, inputHtml: HTMLInputElement) {
    const texto = inputHtml.value;
    if (!texto.trim()) return;

    this.communityService.postComment(item.id, texto).subscribe({
      next: (novoComentario) => {
        item.comments = [...(item.comments || []), novoComentario];
        inputHtml.value = ''; 
        this.cdr.detectChanges();
      },
      error: () => alert('Erro ao comentar')
    });
  }

  hasAction(item: any, type: string): boolean {
    return item.types?.includes(type);
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