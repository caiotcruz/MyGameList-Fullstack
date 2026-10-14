import { TEXT_LIMITS, reviewLimitError } from '../../shared/text-limits';
import { Feedback, errorMessage } from '../../services/feedback';
import { ReviewDrafts } from '../../services/review-drafts';
import { Component, inject, OnInit, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GameService, RecommendationResponse } from '../../services/game';
import { CommunityService } from '../../services/community';
import { UserService } from '../../services/user';
import { catchError, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { RouterModule, Router } from '@angular/router';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterModule],
  templateUrl: './search.html',
  styleUrl: './search.css'
})
export class Search implements OnInit {
  feedback=inject(Feedback);
  drafts=inject(ReviewDrafts);
  readonly limits = TEXT_LIMITS;
  saveError='';
  draftRestored=false;
  preserveDraft(){this.drafts.save(this.editingGame);}
  discardDraft(){this.drafts.clear(this.editingGame.rawgId);this.isModalOpen=false;this.saveError='';}

  gameService = inject(GameService);
  communityService = inject(CommunityService);
  userService = inject(UserService);
  cdr = inject(ChangeDetectorRef);
  router = inject(Router);

  private destroyRef = inject(DestroyRef);
  recommendations: RecommendationResponse | null = null;
  recommendationsLoading = false;
  recommendationsError = '';
  private recommendationRequest = 0;
  readonly genreNames: Record<string,string> = {
    action:'Ação', indie:'Indie', adventure:'Aventura', 'role-playing-games-rpg':'RPG', strategy:'Estratégia', shooter:'Tiro', casual:'Casual', simulation:'Simulação', puzzle:'Puzzle', arcade:'Arcade', platformer:'Plataforma', racing:'Corrida', sports:'Esportes', fighting:'Luta', family:'Família', 'board-games':'Tabuleiro', educational:'Educacional', card:'Cartas', 'massively-multiplayer':'Multijogador massivo'
  };

  query = '';
  games: any[] = [];
  trendingGames: any[] = [];
  displayTrendingGames: any[] = [];
  currentPage = 1;
  isLoading = false;
  isSaving = false;

  userGameIds: Set<number> = new Set();
  private searchSubject = new Subject<string>();

  isModalOpen = false;
  editingGame: any = {};

  ngOnInit() {
    this.carregarColecaoUsuario();
    this.carregarTrending();
    this.carregarRecomendacoes();

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(termo => {
          if (!termo.trim()) {
            return of([]);
          }

          this.isLoading = true;
          return this.gameService.searchGames(termo, 1).pipe(catchError(() => {
            this.isLoading = false;
            return of([]);
          }));
        })
    ).subscribe(resultados => {
        this.games = resultados;
        this.isLoading = false;
        this.cdr.detectChanges();
    });
  }

  get selectedGenreSlugs(): string[] { return this.recommendations?.genres.map(g=>g.genre) || []; }
  genreLabel(slug: string): string { return this.genreNames[slug] || slug; }
  recommendationReason(genres: string[]): string { return genres.map(g=>this.genreLabel(g)).join(' + '); }
  carregarRecomendacoes() {
    if (!localStorage.getItem('token')) return;
    const request=++this.recommendationRequest;
    this.recommendationsLoading=true;this.recommendationsError='';
    this.gameService.getRecommendations().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response=>{
        if(request!==this.recommendationRequest)return;
        this.recommendations={...response,games:response.games.filter(g=>!this.userGameIds.has(g.rawgId))};
        this.recommendationsLoading=false;this.cdr.detectChanges();
      },
      error: ()=>{
        if(request!==this.recommendationRequest)return;
        this.recommendationsLoading=false;
        this.recommendationsError='Não foi possível carregar suas recomendações agora.';
        this.cdr.detectChanges();
      }
    });
  }

  carregarTrending() {
    this.gameService.getTrendingGames(10).subscribe({
      next: (res: any[]) => {
        this.trendingGames = res || [];
        this.displayTrendingGames = [...this.trendingGames, ...this.trendingGames];
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erro ao carregar trending:', err)
    });
  }

  carregarColecaoUsuario() {
    const userId = localStorage.getItem('userId');
    if (userId) {
      this.communityService.getUserList(Number(userId)).subscribe({
        next: (dados: any[]) => {
          this.userGameIds = new Set(dados.map(item => item.game.rawgId));
          if(this.recommendations)this.recommendations.games=this.recommendations.games.filter(g=>!this.userGameIds.has(g.rawgId));
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Erro ao carregar coleção:', err)
      });
    }
  }

  onSearchInput(event: Event) {
    const elemento = event.target as HTMLInputElement;
    this.searchSubject.next(elemento.value);
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
    const id = game.rawgId || game.id;
    if (this.estaNaLista(id)) return;

    this.editingGame = {
      rawgId: id,
      title: game.title || game.name,
      status: 'PLAN_TO_PLAY',
      score: 0,
      review: '',
      isSpoiler: false
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

  verificarStatus() {
    if (this.editingGame.status === 'PLAN_TO_PLAY') {
      this.editingGame.score = 0;
      // Keep the review draft when changing status.
      this.editingGame.isSpoiler = false;
    }
  }

  validarScore() {
    this.editingGame.score = Math.min(10, Math.max(0, Math.floor(this.editingGame.score || 0)));
  }

  salvar() {
    if (this.isSaving) return;
    this.validarScore();
    if(this.isSaving)return;
    this.saveError=reviewLimitError(this.editingGame.review);this.preserveDraft();
    if(this.saveError)return;
    this.isSaving = true;

    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        this.userGameIds.add(this.editingGame.rawgId);
        if(this.recommendations)this.recommendations.games=this.recommendations.games.filter(g=>!this.userGameIds.has(g.rawgId));
        this.carregarRecomendacoes();
        this.isSaving=false;
        this.drafts.clear(this.editingGame.rawgId);
        this.fecharModal(true);
        this.isSaving = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.saveError=errorMessage(err);
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }
}
