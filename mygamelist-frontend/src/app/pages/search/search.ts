import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core'; 
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game';
import { CommunityService } from '../../services/community';
import { UserService } from '../../services/user';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { RouterModule, Router } from '@angular/router';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterModule],
  templateUrl: './search.html',
  styleUrl: './search.css'
})
export class Search implements OnInit {
  gameService = inject(GameService);
  communityService = inject(CommunityService);
  userService = inject(UserService);
  cdr = inject(ChangeDetectorRef); 
  router = inject(Router);
  
  query = '';
  games: any[] = [];
  currentPage = 1;
  isLoading = false;
  isSaving = false;

  userGameIds: Set<number> = new Set();
  private searchSubject = new Subject<string>();

  isModalOpen = false;
  editingGame: any = {};

  ngOnInit() {
    this.carregarColecaoUsuario();

    this.searchSubject.pipe(
      debounceTime(500), 
      distinctUntilChanged() 
    ).subscribe(termo => {
       this.executarBusca(termo);
    });
  }

  carregarColecaoUsuario() {
    const userId = localStorage.getItem('userId');
    if (userId) {
      this.communityService.getUserList(Number(userId)).subscribe({
        next: (dados: any[]) => {
          this.userGameIds = new Set(dados.map(item => item.game.rawgId));
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Erro ao carregar coleção:', err)
      });
    }
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

  estaNaLista(gameId: number): boolean {
    return this.userGameIds.has(gameId);
  }

  adicionar(game: any) {
    if (this.estaNaLista(game.id)) return;

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
    this.editingGame.score = Math.min(10, Math.max(0, Math.floor(this.editingGame.score || 0)));
  }

  salvar() {
    if (this.isSaving) return;
    this.validarScore();
    this.isSaving = true; 

    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        this.userGameIds.add(this.editingGame.rawgId);
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