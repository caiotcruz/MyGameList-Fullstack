import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { DatePipe, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // 👈 Importante para o input
import { ActivityService } from '../../services/activity'; 
import { CommunityService } from '../../services/community'; // 👈 Serviço de Likes/Comments
import { Activity } from '../../models/activity.model';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule], // 👈 Adicione FormsModule aqui
  templateUrl: './feed.html',
  styleUrl: './feed.css'
})
export class Feed implements OnInit {
  activityService = inject(ActivityService);
  communityService = inject(CommunityService); // 👈 Injete aqui
  cdr = inject(ChangeDetectorRef); 

  activities: any[] = []; // Mudei para any[] temporariamente para aceitar likes/comments sem erro de tipagem
  myId: number = 0;

  ngOnInit() {
    // Pegar meu ID para saber se o like é meu
    this.myId = Number(localStorage.getItem('userId'));

    this.activityService.getFeed().subscribe({
      next: (data) => {
        this.activities = data;
        this.cdr.detectChanges(); 
      },
      error: (err) => console.error('Erro ao carregar feed', err)
    });
  }

  // --- LÓGICA DE INTERAÇÃO (LIKE/COMMENT) ---

  isLikedByMe(item: any): boolean {
    if (!item.likes) return false;
    return item.likes.some((l: any) => l.user.id === this.myId);
  }

  toggleLike(item: any) {
    // 1. Otimismo Visual
    const jaCurtiu = this.isLikedByMe(item);
    
    if (jaCurtiu) {
      item.likes = item.likes.filter((l: any) => l.user.id !== this.myId);
    } else {
      if (!item.likes) item.likes = [];
      item.likes.push({ user: { id: this.myId } }); 
    }

    // 2. Chama Backend
    this.communityService.toggleLike(item.id).subscribe({
      error: () => {
        alert('Erro ao curtir');
        // Rollback simples se precisar, ou recarregar feed
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

  // --- MÉTODOS VISUAIS ANTIGOS ---

  getActionText(type: string): string {
    switch(type) {
      case 'ADDED_TO_LIST': return 'adicionou à biblioteca';
      case 'CHANGED_STATUS': return 'alterou o status para';
      case 'RATED': return 'deu nota';
      case 'REVIEWED': return 'fez uma review de';
      default: return 'atualizou';
    }
  }
  
  formatDetail(type: string, detail: string): string {
    if (type === 'RATED') return `⭐ ${detail}/10`;
    if (type === 'CHANGED_STATUS') return detail?.replace('_', ' '); 
    return detail || '';
  }
}