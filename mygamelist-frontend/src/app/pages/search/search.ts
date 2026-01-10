import { Component, inject, ChangeDetectorRef } from '@angular/core'; // <--- 1. Importe ChangeDetectorRef
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
  cdr = inject(ChangeDetectorRef); // <--- 2. Injete a ferramenta de detecção
  
  query = '';
  games: any[] = [];
  currentPage = 1;
  isLoading = false;

  buscar() {
    if (!this.query) return;
    
    console.log('Iniciando busca...');
    this.currentPage = 1;
    this.isLoading = true;
    this.games = []; // Limpa a lista visualmente para dar feedback que está buscando nova

    this.gameService.searchGames(this.query, this.currentPage).subscribe({
      next: (resultados) => {
        console.log('Dados chegaram! Qtd:', resultados.length);
        this.games = resultados;
        this.isLoading = false;

        // <--- 3. A MÁGICA: OBRIGA O HTML A ATUALIZAR AGORA
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        console.error('Deu erro:', err);
        this.isLoading = false;
        this.cdr.detectChanges(); // Força atualização no erro também
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
        this.cdr.detectChanges(); // <--- Força atualização aqui também
      },
      error: (err) => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  adicionar(game: any) {
    // ... seu código de adicionar (pode manter igual) ...
    const payload = {
      rawgId: game.id,
      status: 'PLAN_TO_PLAY',
      score: 0,
      review: ''
    };
    this.gameService.addGameToList(payload).subscribe({
        next: () => alert(`"${game.name}" adicionado!`),
        error: (err) => alert('Erro ao adicionar.')
    });
  }
}