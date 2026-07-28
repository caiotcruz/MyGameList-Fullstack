import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { Feed } from '../../components/feed/feed'; 
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user';
import { CommunityService } from '../../services/community';
import { GameService } from '../../services/game';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, Feed],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {
  authService = inject(AuthService);
  userService = inject(UserService);
  communityService = inject(CommunityService);
  gameService = inject(GameService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef); 
  
  userName = 'Gamer'; 
  myId!: number;
  
  userLevel = 1;
  userExperience = 0;
  readonly xpPerLevel = 1000;

  currentlyPlayingGame: any = null;
  trendingGame: any = null;

  ngOnInit(): void {
    this.updateUserName();
    this.myId = Number(localStorage.getItem('userId'));
    if (this.myId) {
      this.carregarDadosUsuario();
      this.carregarJogoEmAlta();
    }
  }

  updateUserName() {
    const storedName = localStorage.getItem('userName');
    if (storedName) {
      this.userName = storedName;
    }
    this.cdr.detectChanges();
  }

  carregarDadosUsuario() {
    this.userService.getById(this.myId).subscribe({
      next: (user: any) => {
        if (user) {
          this.userName = user.name || this.userName;
          this.userLevel = user.level ?? 1;
          this.userExperience = user.experience ?? 0;
          this.cdr.detectChanges();
        }
      }
    });

    this.communityService.getUserList(this.myId).subscribe({
      next: (games: any[]) => {
        if (games && games.length > 0) {
          this.currentlyPlayingGame = games.find(g => g.status === 'PLAYING') || null;
          this.cdr.detectChanges();
        }
      }
    });
  }

  carregarJogoEmAlta() {
    this.gameService.getTrendingGames(1).subscribe({
      next: (res: any[]) => {
        if (res && res.length > 0) {
          this.trendingGame = res[0];
          this.cdr.detectChanges();
        }
      },
      error: (err) => console.error('Erro ao carregar jogo em alta:', err)
    });
  }

  get xpPercentage(): number {
    return Math.min(100, Math.max(0, (this.userExperience / this.xpPerLevel) * 100));
  }
}