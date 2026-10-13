import { Feedback, errorMessage } from '../../services/feedback';
import { ReviewDrafts } from '../../services/review-drafts';
import { Component, Input, Output, EventEmitter, inject, ChangeDetectorRef, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GameService } from '../../services/game';
import { RouterModule, Router } from '@angular/router';

@Component({
  selector: 'app-game-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './game-list.html',
  styleUrl: './game-list.css'
})
export class GameList implements OnChanges {
  feedback=inject(Feedback);
  drafts=inject(ReviewDrafts);
  saveError='';
  draftRestored=false;
  preserveDraft(){this.drafts.save(this.editingGame);}
  discardDraft(){this.drafts.clear(this.editingGame.rawgId);this.isModalOpen=false;this.saveError='';}

  gameService = inject(GameService);
  cdr = inject(ChangeDetectorRef);
  router = inject(Router);

  @Input() games: any[] = [];
  @Input() isOwner: boolean = false;
  @Output() listUpdated = new EventEmitter<void>();

  stats = { total: 0, completed: 0, playing: 0, platinum: 0 };
  isModalOpen = false;
  editingGame: any = {};
  isSaving = false;
  favoriteBusy=false;
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  reviewSelecionada: any = null;
  gameParaRemover: any = null;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['games'] && this.games) {
      this.aplicarOrdenacaoPadrao();
      this.calcularStats();
    }
  }

  aplicarOrdenacaoPadrao() {
    this.sortColumn = ''; 
    this.games.sort((a, b) => {
      if (a.favorite && !b.favorite) return -1;
      if (!a.favorite && b.favorite) return 1;

      const dateA = new Date(a.updatedAt || 0).getTime();
      const dateB = new Date(b.updatedAt || 0).getTime();
      return dateB - dateA;
    });
  }

  limparOrdenacao() {
    this.aplicarOrdenacaoPadrao();
    this.cdr.detectChanges();
  }

  ordenar(coluna: string) {
    if (this.sortColumn === coluna) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = coluna;
      this.sortDirection = 'asc';
    }

    this.games.sort((a, b) => {
      let vA, vB;
      if (coluna === 'title') { vA = a.game.title.toLowerCase(); vB = b.game.title.toLowerCase(); }
      else if (coluna === 'status') { vA = a.status; vB = b.status; }
      else if (coluna === 'score') { vA = a.score || 0; vB = b.score || 0; }
      else return 0;

      if (vA < vB) return this.sortDirection === 'asc' ? -1 : 1;
      if (vA > vB) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  getSortIcon(coluna: string): string {
    if (this.sortColumn !== coluna) return '↕'; 
    return this.sortDirection === 'asc' ? '▲' : '▼';
  }

  toggleFavorite(item: any, event: Event) {
    event.stopPropagation();
    event.preventDefault();
    if(this.favoriteBusy)return;
    this.favoriteBusy=true;
    const novoStatus=!item.favorite;
    this.gameService.addGameToList({rawgId:item.game.rawgId,isFavorite:novoStatus}).subscribe({
      next:()=>{this.favoriteBusy=false;this.listUpdated.emit();this.cdr.detectChanges();},
      error:err=>{this.favoriteBusy=false;this.feedback.error(err);this.cdr.detectChanges();}
    });
  }

  navegarParaJogo(item: any) { 
    this.router.navigate(['/game', item.game.rawgId]); 
  }
  
  getStatusColor(status: string): string {
    const colors: any = { 
      'PLAYING': '#4caf50', 
      'COMPLETED': '#2196f3', 
      'PLATINUM': '#00e5ff',
      'DROPPED': '#f44336', 
      'PLAN_TO_PLAY': '#9e9e9e' 
    };
    return colors[status] || '#000';
  }

  abrirEdicao(item: any, event: Event) {
    event.stopPropagation();
    const temSpoiler = Boolean(item.spoiler || item.isSpoiler || item.reviewIsSpoiler);

    this.editingGame = { 
      rawgId: item.game.rawgId, 
      title: item.game.title, 
      status: item.status, 
      score: item.score, 
      review: item.review,
      isSpoiler: temSpoiler
    };
    const recovered=this.drafts.restore(this.editingGame.rawgId);
    this.draftRestored=!!recovered;
    if(recovered)this.editingGame={...this.editingGame,...recovered};
    this.saveError='';
    this.isModalOpen = true;
  }

  fecharModal(saved=false) {
    if(this.isSaving)return;
    if(!saved)this.preserveDraft(); 
    this.isModalOpen = false; 
  }

  validarScore() { 
    this.editingGame.score = Math.min(10, Math.max(0, Math.floor(this.editingGame.score || 0))); 
  }

  salvarAlteracoes() {
    if(this.isSaving)return;
    this.saveError="";this.preserveDraft();
    this.isSaving = true;
    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        this.isSaving = false;
        this.drafts.clear(this.editingGame.rawgId);
        this.fecharModal(true);
        this.listUpdated.emit();
      },
      error: (err) => {this.isSaving=false;this.saveError=errorMessage(err);this.cdr.detectChanges();}
    });
  }

  removerJogo(item: any, event?: Event) {
    if (event) event.stopPropagation();
    this.gameParaRemover = item;
  }

  confirmarRemocao() {
    if (!this.gameParaRemover) return;

    this.gameService.deleteGame(this.gameParaRemover.id).subscribe({
      next: () => {
        this.listUpdated.emit();
        this.gameParaRemover = null;
      },
      error: () => {
        this.feedback.show('Erro ao remover jogo.');
        this.gameParaRemover = null;
      }
    });
  }

  cancelarRemocao() {
    this.gameParaRemover = null;
  }

  lerReview(item: any, event: Event) {
    event.stopPropagation();
    const temSpoiler = Boolean(item.spoiler || item.isSpoiler || item.reviewIsSpoiler);
    this.reviewSelecionada = { 
      title: item.game.title, 
      text: item.review, 
      score: item.score,
      isSpoiler: temSpoiler,
      showSpoilerText: false 
    };
  }

  toggleSpoilerReviewModal() {
    if (this.reviewSelecionada) {
      this.reviewSelecionada.showSpoilerText = !this.reviewSelecionada.showSpoilerText;
      this.cdr.detectChanges();
    }
  }

  fecharReview() { 
    this.reviewSelecionada = null; 
  }

  trackByGameId(index: number, item: any) { 
    return item.id; 
  }

  calcularStats() {
    this.stats.total = this.games.length;
    this.stats.completed = this.games.filter(g => g.status === 'COMPLETED').length;
    this.stats.playing = this.games.filter(g => g.status === 'PLAYING').length;
    this.stats.platinum = this.games.filter(g => g.status === 'PLATINUM').length;
  }
}