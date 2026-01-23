import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core'; 
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterModule],
  templateUrl: './search.html',
  styleUrl: './search.css'
})
export class Search implements OnInit {
  gameService = inject(GameService);
  cdr = inject(ChangeDetectorRef); 
  
  query = '';
  games: any[] = [];
  currentPage = 1;
  isLoading = false;
  isSaving = false;

  private searchSubject = new Subject<string>();

  isModalOpen = false;
  editingGame: any = {};

  ngOnInit() {
    this.searchSubject.pipe(
      debounceTime(500), 
      distinctUntilChanged() 
    ).subscribe(termo => {
       this.executarBusca(termo);
    });
  }

  onSearchInput(termo: string) {
    this.searchSubject.next(termo);
  }

  executarBusca(termo: string) {
    if (!termo.trim()) {
        this.games = [];
        this.isLoading = false;
        return;
    }

    console.log('Iniciando busca automática para:', termo);
    this.currentPage = 1;
    this.isLoading = true;
    this.games = []; 

    this.gameService.searchGames(termo, this.currentPage).subscribe({
      next: (resultados) => {
        this.games = resultados;
        this.isLoading = false;
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        console.error('Erro na busca:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  carregarMais() {
    this.currentPage++;
    this.isLoading = true;

    this.gameService.searchGames(this.query, this.currentPage).subscribe({
      next: (novosResultados) => {
        this.games.push(...novosResultados);
        this.isLoading = false;
        this.cdr.detectChanges(); 
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  adicionar(game: any) {
    this.editingGame = {
      rawgId: game.id,
      title: game.name,
      status: 'PLAN_TO_PLAY', 
      score: 0,
      review: ''
    };
    this.isModalOpen = true;
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
    if (this.editingGame.score) {
        this.editingGame.score = Math.floor(this.editingGame.score);
    }
    if (this.editingGame.score > 10) this.editingGame.score = 10;
    if (this.editingGame.score < 0) this.editingGame.score = 0;
  }

  salvar() {
    if (this.isSaving) return;

    this.validarScore();
    this.isSaving = true; 

    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        alert('Jogo adicionado!');
        this.fecharModal();
        this.isSaving = false; 
        this.cdr.detectChanges(); 
      },
      error: () => {
        alert('Erro ao adicionar.');
        this.isSaving = false; 
        this.cdr.detectChanges(); 
      }
    });
  }
}