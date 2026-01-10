import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { GameService } from '../../services/game';

@Component({
  selector: 'app-my-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-list.html',
  styleUrl: './my-list.css'
})
export class MyList implements OnInit {
  gameService = inject(GameService);
  cdr = inject(ChangeDetectorRef);
  myGames: any[] = [];
  
  isModalOpen = false;
  editingGame: any = {}; 

  totalJogos = 0;
  totalZerados = 0;
  totalJogando = 0;

  ngOnInit(): void {
    this.carregarLista();
  }

  carregarLista() {
    this.gameService.getMyList().subscribe({
      next: (dados: any) => {
        this.myGames = Array.isArray(dados) ? dados : [dados];
        
        this.totalJogos = this.myGames.length;
        this.totalZerados = this.myGames.filter(g => g.status === 'COMPLETED').length;
        this.totalJogando = this.myGames.filter(g => g.status === 'PLAYING').length;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  abrirEdicao(item: any) {
    this.editingGame = { 
      rawgId: item.game.rawgId, 
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

  salvarAlteracoes() {
    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        alert('Jogo atualizado!');
        this.fecharModal();
        this.carregarLista(); 
      },
      error: (err) => alert('Erro ao atualizar.')
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

  removerJogo(item: any) {
    if (confirm(`Tem certeza que deseja remover "${item.game.title}" da sua lista?`)) {
      this.gameService.deleteGame(item.id).subscribe({
        next: () => {
          alert('Jogo removido!');
          this.carregarLista();
        },
        error: (err) => alert('Erro ao remover.')
      });
    }
  }
}