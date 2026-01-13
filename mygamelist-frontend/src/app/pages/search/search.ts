import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core'; 
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, CommonModule],
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

  // Controle do Debounce (Busca Dinâmica)
  private searchSubject = new Subject<string>();

  isModalOpen = false;
  editingGame: any = {};

  ngOnInit() {
    // Configura o "tubo" de pesquisa
    this.searchSubject.pipe(
      debounceTime(500), // Espera 500ms o usuário parar de digitar
      distinctUntilChanged() // Só busca se o texto mudou
    ).subscribe(termo => {
       this.executarBusca(termo);
    });
  }

  // Chamado pelo input no HTML a cada letra digitada
  onSearchInput(termo: string) {
    this.searchSubject.next(termo);
  }

  // Lógica central da busca
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
      status: 'PLAN_TO_PLAY', // Padrão seguro
      score: 0,
      review: ''
    };
    this.isModalOpen = true;
  }

  fecharModal() {
    this.isModalOpen = false;
  }

  // 👇 NOVA LÓGICA: Se mudar para "Planejo Jogar", zera nota e review
  verificarStatus() {
    if (this.editingGame.status === 'PLAN_TO_PLAY') {
       this.editingGame.score = 0;
       this.editingGame.review = '';
    }
  }

  // 👇 NOVA LÓGICA: Garante que a nota seja inteira (0, 1, ... 10)
  validarScore() {
    if (this.editingGame.score) {
        this.editingGame.score = Math.floor(this.editingGame.score);
    }
    if (this.editingGame.score > 10) this.editingGame.score = 10;
    if (this.editingGame.score < 0) this.editingGame.score = 0;
  }

  salvar() {
    if (this.isSaving) return; // Segurança extra

    this.validarScore();
    this.isSaving = true; // 🔒 Bloqueia

    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        alert('Jogo adicionado!');
        this.fecharModal();
        this.isSaving = false; // 🔓 Libera
        this.cdr.detectChanges(); 
      },
      error: () => {
        alert('Erro ao adicionar.');
        this.isSaving = false; // 🔓 Libera em caso de erro também
        this.cdr.detectChanges(); 
      }
    });
  }
}