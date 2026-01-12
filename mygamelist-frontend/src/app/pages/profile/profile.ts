import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { CommunityService } from '../../services/community';
import { GameList } from '../../components/game-list/game-list'; 
import { AuthService } from '../../services/auth'; 

interface Badge {
  icon: string;
  label: string;
  color: string;
  description: string;
}

interface ChartBar {
  score: number;
  count: number;   
  heightPc: number; 
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, GameList],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {
  route = inject(ActivatedRoute);
  communityService = inject(CommunityService);
  authService = inject(AuthService); 
  cdr = inject(ChangeDetectorRef);

  userId: number = 0;
  userGames: any[] = [];
  userName: string = 'Carregando...';
  isMyProfile: boolean = false;

  stats = {
    total: 0,
    completed: 0,
    playing: 0,
    avgScore: 0
  };

  badges: Badge[] = [];
  chartData: ChartBar[] = [];
  favoriteGame: any = null; 

  ngOnInit() {
    this.userId = Number(this.route.snapshot.paramMap.get('id'));
    const myId = localStorage.getItem('userId'); 
    this.isMyProfile = (myId && Number(myId) === this.userId) || false;
    if (this.userId) {
      this.carregarPerfil();
    }
  }

  carregarPerfil() {
    this.communityService.getUserList(this.userId).subscribe({
      next: (dados: any) => {
        this.userGames = dados;
        if (this.userGames.length > 0) {
           this.userName = this.userGames[0].user.name;
           this.calcularHallDaFama();
        }
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  onListUpdated() {
    console.log('🔄 Atualizando Destaque...');
    
    this.favoriteGame = null; 
    
    setTimeout(() => {
        this.calcularHallDaFama();
        this.cdr.detectChanges(); 
    }, 0);
  }

  calcularHallDaFama() {

    this.stats.total = this.userGames.length;
    this.stats.completed = this.userGames.filter(g => g.status === 'COMPLETED').length;
    this.stats.playing = this.userGames.filter(g => g.status === 'PLAYING').length;

    const ratedGames = this.userGames.filter(g => g.score > 0);

    const counts = new Array(11).fill(0); 
    let maxCount = 0;

    for (const game of ratedGames) {
        counts[game.score]++;
        if (counts[game.score] > maxCount) {
            maxCount = counts[game.score];
        }
    }

    this.chartData = [];
    for (let i = 1; i <= 10; i++) {
        const percentage = maxCount > 0 ? (counts[i] / maxCount * 100) : 0;
        
        this.chartData.push({
            score: i,
            count: counts[i],
            heightPc: percentage
        });
    }

    const totalScore = ratedGames.reduce((sum, g) => sum + g.score, 0);
    this.stats.avgScore = ratedGames.length ? (totalScore / ratedGames.length) : 0;
    const manualFavorite = this.userGames.find(g => g.favorite === true);

    if (manualFavorite) {
        console.log('Achou favorito manual:', manualFavorite.game.title);
        this.favoriteGame = manualFavorite;
    } else {
        if (ratedGames.length > 0) {
            this.favoriteGame = ratedGames.sort((a, b) => b.score - a.score)[0];
        } else {
            this.favoriteGame = null;
        }
    }

    this.badges = [];

    // Colecionador
    if (this.stats.total >= 1) this.badges.push({ 
        icon: '🎲', label: 'Iniciante', color: '#cd7f32', 
        description: 'Adicionou o primeiro jogo à coleção.' 
    });
    if (this.stats.total >= 10) this.badges.push({ 
        icon: '📚', label: 'Bibliotecário', color: 'silver', 
        description: 'Possui mais de 10 jogos na coleção.' 
    });
    if (this.stats.total >= 50) this.badges.push({ 
        icon: '👑', label: 'Lendário', color: 'gold', 
        description: 'Uma lenda! Possui mais de 50 jogos.' 
    });

    // Zerador
    if (this.stats.completed >= 5) this.badges.push({ 
        icon: '🏆', label: 'Zerador Elite', color: '#4caf50', 
        description: 'Completou (zerou) 5 ou mais jogos.' 
    });
    if (this.stats.completed >= 20) this.badges.push({ 
        icon: '💀', label: 'Hardcore', color: '#d32f2f', 
        description: 'Um mestre! Completou mais de 20 jogos.' 
    });

    // Crítico
    const reviewsCount = this.userGames.filter(g => g.review && g.review.length > 5).length;
    if (reviewsCount >= 3) this.badges.push({ 
        icon: '✍️', label: 'Crítico', color: '#2196f3', 
        description: 'Escreveu reviews para 3 ou mais jogos.' 
    });
    
    // Qualidade
    if (this.stats.avgScore >= 9 && ratedGames.length >= 3) {
        this.badges.push({ 
            icon: '⭐', label: 'Bom Gosto', color: '#ffbf00', 
            description: 'Mantém uma média de notas acima de 9.' 
        });
    }
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
}