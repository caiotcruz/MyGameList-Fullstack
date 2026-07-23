import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms'; 
import { GameService, GameHubData } from '../../services/game';

@Component({
  selector: 'app-game-details',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './game-details.html', 
  styleUrl: './game-details.css'
})
export class GameDetails implements OnInit {
  route = inject(ActivatedRoute);
  gameService = inject(GameService);
  cdr = inject(ChangeDetectorRef);

  data: GameHubData | null = null;
  isLoading = true;
  
  isModalOpen = false;
  isSaving = false;
  editingGame: any = {};

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) this.carregarHub(id);
    });
  }

  carregarHub(id: string) {
    this.isLoading = true;
    this.gameService.getGameHub(id).subscribe({
      next: (res) => {
        this.data = res;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }


  abrirModal() {
    if (!this.data) return;

    const userId = localStorage.getItem('userId');
    let myExistingReview = '';

    if (userId && this.data.latestReviews) {
      const userReview = this.data.latestReviews.find(r => r.myVote !== undefined); 
      if (userReview) {
        myExistingReview = userReview.review;
      }
    }

    this.editingGame = {
      rawgId: this.data.externalId,
      title: this.data.title,
      coverUrl: this.data.coverUrl,
      status: this.data.userStatus || 'PLAN_TO_PLAY',
      score: this.data.userScore || 0,
      review: myExistingReview
    };

    this.isModalOpen = true;
    this.cdr.detectChanges();
  }

  fecharModal() {
    this.isModalOpen = false;
  }

  verificarStatus() {
    if (this.editingGame.status === 'PLAN_TO_PLAY') {
      this.editingGame.score = 0;
      this.editingGame.review = '';
    }
  }

  validarScore() {
    this.editingGame.score = Math.min(10, Math.max(0, Math.floor(this.editingGame.score || 0)));
  }

  salvar() {
    if (this.isSaving) return;
    this.validarScore();
    this.isSaving = true;

    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        this.isSaving = false;
        this.fecharModal();
        if (this.data) this.carregarHub(this.data.externalId.toString());
      },
      error: (err) => {
        alert('Erro ao salvar. Verifique se você está logado.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  
  getScoreColor(score: number): string {
    if (score >= 9) return '#4caf50';
    if (score >= 7) return '#2196f3';
    if (score >= 5) return '#ff9800';
    return '#f44336';
  }

  getStatusLabel(status: string): string {
    const map: any = { 
      'PLAYING': 'Jogando', 
      'COMPLETED': 'Zerado', 
      'PLATINUM': 'Platinado',
      'PLAN_TO_PLAY': 'Quero Jogar', 
      'DROPPED': 'Larguei' 
    };
    return map[status] || status;
  }

  votar(rev: any, type: 'LIKE' | 'DISLIKE') {
    const reviewId = rev.reviewId || rev.id;

    if (!reviewId) {
      console.error("❌ ERRO CRÍTICO: ID da review veio nulo/undefined!", rev);
      return;
    }

    const oldVote = rev.myVote;
    if (rev.myVote === type) {
        rev.myVote = null;
        if (type === 'LIKE') rev.likesCount--;
        else rev.dislikesCount--;
    } else {
        if (rev.myVote === 'LIKE') rev.likesCount--;
        if (rev.myVote === 'DISLIKE') rev.dislikesCount--;
        
        rev.myVote = type;
        if (type === 'LIKE') rev.likesCount++;
        else rev.dislikesCount++;
    }
    
    rev.voteScore = (rev.likesCount * 2) - rev.dislikesCount;

    this.gameService.voteReview(reviewId, type).subscribe({
        error: (err) => {
            console.error('Erro no voto:', err);
        }
    });
  }
}