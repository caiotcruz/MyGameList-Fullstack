import { Component, inject, ChangeDetectorRef } from '@angular/core'; 
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './search.html',
  styleUrl: './search.css'
})
export class Search {
  gameService = inject(GameService);
  cdr = inject(ChangeDetectorRef); 
  
  query = '';
  games: any[] = [];
  currentPage = 1;
  isLoading = false;

  isModalOpen = false;
  editingGame: any = {};

  buscar() {
    if (!this.query) return;
    
    console.log('Iniciando busca...');
    this.currentPage = 1;
    this.isLoading = true;
    this.games = []; 

    this.gameService.searchGames(this.query, this.currentPage).subscribe({
      next: (resultados) => {
        console.log('Dados chegaram! Qtd:', resultados.length);
        this.games = resultados;
        this.isLoading = false;

        this.cdr.detectChanges(); 
      },
      error: (err) => {
        console.error('Deu erro:', err);
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
      error: (err) => {
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

  salvar() {
    if (this.editingGame.score < 0 || this.editingGame.score > 10) {
      alert('Nota inválida');
      return;
    }

    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        alert('Jogo adicionado!');
        this.fecharModal();
        
        this.cdr.detectChanges(); 
      },
      error: () => {
        alert('Erro ao adicionar.');
        this.cdr.detectChanges(); 
      }
    });
  }

}