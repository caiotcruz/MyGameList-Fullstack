import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { DatePipe, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { ActivityService } from '../../services/activity'; 
import { CommunityService } from '../../services/community'; 
import { Activity } from '../../models/activity.model';
import { RouterModule } from '@angular/router';

export interface GroupedActivity {
  id: number;
  user: any;
  game: any;
  timestamp: any;
  types: string[];
  details: { [key: string]: string }; 
  likes: any[];
  comments: any[];
  showComments?: boolean;
}

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
        this.activities = this.groupActivities(data);
        this.cdr.detectChanges(); 
      }
    });
  }

  groupActivities(data: any[]): any[] {
  const grouped: any[] = [];
  
  data.forEach(activity => {
    const existing = grouped.find(g => 
      g.user.id === activity.user.id && 
      g.game.rawgId === activity.game.rawgId &&
      Math.abs(new Date(g.timestamp).getTime() - new Date(activity.timestamp).getTime()) < 10000
    );

    if (existing) {
      existing.allActions.push({ type: activity.type, detail: activity.detail });
      
      if (activity.type === 'RATED') {
        existing.ratingForBadge = activity.detail;
      }
      if (activity.type === 'REVIEWED') {
        existing.fullReview = activity.detail;
      }
    } else {
      grouped.push({
        ...activity,
        allActions: [{ type: activity.type, detail: activity.detail }],
        ratingForBadge: activity.type === 'RATED' ? activity.detail : null,
        fullReview: activity.type === 'REVIEWED' ? activity.detail : null,
        showComments: false 
      });
    }
  });
  return grouped;
}
  isLikedByMe(item: any): boolean {
    if (!item.likes) return false;
    return item.likes.some((l: any) => l.user.id === this.myId);
  }

  toggleLike(item: any) {
    const jaCurtiu = this.isLikedByMe(item);
    
    if (jaCurtiu) {
      item.likes = item.likes.filter((l: any) => l.user.id !== this.myId);
    } else {
      if (!item.likes) item.likes = [];
      item.likes.push({ user: { id: this.myId } }); 
    }

    this.communityService.toggleLike(item.id).subscribe({
      error: () => {
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
        const comentariosAtuais = item.comments || [];

        item.comments = [...comentariosAtuais, novoComentario];
        
        inputHtml.value = ''; 
        this.cdr.detectChanges();
      },
      error: () => alert('Erro ao comentar')
    });
  }

  hasAction(item: any, type: string): boolean {
    return item.allActions?.some((a: any) => a.type === type);
  }

  getActionDetail(item: any, type: string): string {
    const action = item.allActions?.find((a: any) => a.type === type);
    return action ? action.detail : '';
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
  
  formatDetail(type: string, detail: string): string {
    if (type === 'RATED') return `⭐ ${detail}/10`;
    if (type === 'CHANGED_STATUS') return detail?.replace('_', ' '); 
    return detail || '';
  }
}