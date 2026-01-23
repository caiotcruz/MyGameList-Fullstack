import { Component, Input, Output, EventEmitter, inject, ChangeDetectorRef, OnChanges, SimpleChanges } from '@angular/core';import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GameService } from '../../services/game';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-game-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './game-list.html',
  styleUrl: './game-list.css'
})
export class GameList implements OnChanges {
  gameService = inject(GameService);
  cdr = inject(ChangeDetectorRef);

  @Input() games: any[] = [];
  @Input() isOwner: boolean = false;
  @Output() listUpdated = new EventEmitter<void>();

  stats = { total: 0, completed: 0, playing: 0 };

  isModalOpen = false;
  editingGame: any = {};
  isSaving = false;

  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  reviewSelecionada: any = null;
item: any;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['games'] && this.games) {
      this.calcularStats();
    }
  }

  calcularStats() {
    this.stats.total = this.games.length;
    this.stats.completed = this.games.filter(g => g.status === 'COMPLETED').length;
    this.stats.playing = this.games.filter(g => g.status === 'PLAYING').length;
  }

  toggleFavorite(item: any, event: Event) {
    event.stopPropagation();
    event.preventDefault();

    console.log('--- INÍCIO CLICK FAVORITO ---');
    console.log('Estado anterior:', item.favorite);

    const novoStatus = !item.favorite;

    if (novoStatus === true) {
        this.games.forEach(g => g.favorite = false);
    }

    item.favorite = novoStatus;
    
    console.log('Estado novo aplicado na memória:', item.favorite);

    this.cdr.detectChanges(); 

    this.listUpdated.emit(); 

    const updateDto = {
      rawgId: item.game.rawgId || item.game.id,
      isFavorite: novoStatus
    };

    this.gameService.addGameToList(updateDto).subscribe({
      next: () => console.log('✅ Salvo no backend com sucesso'),
      error: (err) => {
        console.error('❌ Erro no backend', err);
        item.favorite = !novoStatus;
        this.cdr.detectChanges();
        alert('Erro ao salvar. Tente novamente.');
      }
    });
  }

  getStatusColor(status: string): string {
    switch(status) {
      case 'PLAYING': return '#4caf50';
      case 'COMPLETED': return '#2196f3';
      case 'DROPPED': return '#f44336';
      case 'PLAN_TO_PLAY': return '#9e9e9e';
      default: return '#000';
    }
  }

  abrirEdicao(item: any) {
    this.editingGame = { 
      rawgId: item.game.rawgId || item.game.id, 
      title: item.game.title, 
      status: item.status,
      score: item.score,
      review: item.review
    };
    this.isModalOpen = true;
  }

  fecharModal() {
    this.isModalOpen = false;
  }

  validarScore() {
    this.editingGame.score = Math.floor(this.editingGame.score);
    if (this.editingGame.score > 10) this.editingGame.score = 10;
    if (this.editingGame.score < 0) this.editingGame.score = 0;
  }

  salvarAlteracoes() {
    if (this.editingGame.score < 0 || this.editingGame.score > 10) {
      alert('Nota inválida');
      return;
    }
    this.isSaving = true;
    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        
        const index = this.games.findIndex(g => 
            (g.game.rawgId || g.game.id) === this.editingGame.rawgId
        );

        if (index !== -1) {
            this.games[index].score = this.editingGame.score;
            this.games[index].status = this.editingGame.status;
            this.games[index].review = this.editingGame.review;
            this.games[index].updatedAt = new Date().toISOString();
        }
        this.isSaving = false; 
        this.fecharModal();

        alert('Jogo atualizado!');
        this.fecharModal();
        
        this.cdr.detectChanges();

        this.listUpdated.emit(); 
      },
      error: () => {
        alert('Erro ao atualizar.')
        this.isSaving = false;
      }
    });
  }

  removerJogo(item: any) {
    if (confirm(`Remover "${item.game.title}"?`)) {
      this.gameService.deleteGame(item.id).subscribe({
        next: () => {
          this.listUpdated.emit();
        },
        error: () => alert('Erro ao remover.')
      });
    }
  }

  ordenar(coluna: string) {
    if (this.sortColumn === coluna) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = coluna;
      this.sortDirection = 'asc';
    }

    this.games.sort((a, b) => {
      let valorA, valorB;

      switch (coluna) {
        case 'title':
          valorA = a.game.title.toLowerCase();
          valorB = b.game.title.toLowerCase();
          break;
        case 'status':
          valorA = a.status;
          valorB = b.status;
          break;
        case 'score':
          valorA = a.score || 0;
          valorB = b.score || 0;
          break;
        case 'favorite':
           valorA = a.favorite ? 1 : 0;
           valorB = b.favorite ? 1 : 0;
           break;
        default:
          return 0;
      }

      if (valorA < valorB) return this.sortDirection === 'asc' ? -1 : 1;
      if (valorA > valorB) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  getSortIcon(coluna: string): string {
    if (this.sortColumn !== coluna) return ''; 
    return this.sortDirection === 'asc' ? '▲' : '▼';
  }

  lerReview(item: any) {
    if (!item.review) return; 
    
      this.reviewSelecionada = {
          title: item.game.title,
          text: item.review,
          score: item.score
      };
    }

    fecharReview() {
        this.reviewSelecionada = null;
    }
}