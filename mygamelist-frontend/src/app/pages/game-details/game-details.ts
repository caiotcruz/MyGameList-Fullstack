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

    this.editingGame = {
      rawgId: this.data.externalId,
      title: this.data.title,
      coverUrl: this.data.coverUrl,
      status: this.data.userStatus || 'PLAN_TO_PLAY',
      score: this.data.userScore || 0,
      review: '' 
    };
    this.isModalOpen = true;
  }

  fecharModal() {
    this.isModalOpen = false;
  }

  validarScore() {
    if (this.editingGame.score > 10) this.editingGame.score = 10;
    if (this.editingGame.score < 0) this.editingGame.score = 0;
  }

  salvar() {
    this.isSaving = true;
    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        alert('Lista atualizada!');
        this.isSaving = false;
        this.fecharModal();
        if (this.data) this.carregarHub(this.data.externalId.toString());
      },
      error: (err) => {
        alert('Erro ao salvar. Você está logado?');
        this.isSaving = false;
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
    const map: any = { 'PLAYING': 'Jogando', 'COMPLETED': 'Zerado', 'PLAN_TO_PLAY': 'Quero Jogar', 'DROPPED': 'Larguei' };
    return map[status] || status;
  }
}