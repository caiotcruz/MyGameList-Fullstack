import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { CommunityService } from '../../services/community';
import { GameList } from '../../components/game-list/game-list'; 
import { AuthService } from '../../services/auth'; 
import { UserService } from '../../services/user'; 

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
  imports: [CommonModule, GameList, FormsModule, RouterModule], 
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit, OnDestroy {
  route = inject(ActivatedRoute);
  communityService = inject(CommunityService);
  authService = inject(AuthService); 
  userService = inject(UserService); 
  cdr = inject(ChangeDetectorRef);

  userId: number = 0;
  userGames: any[] = [];
  userName: string = 'Carregando...';
  userBio: string = '';  
  userAvatar: string = ''; 
  
  isMyProfile: boolean = false;
  isEditingProfile = false; 
  
  editData = { name: '', bio: '', profilePicture: '' };

  statsCounts = { following: 0, followers: 0 };
  stats = { total: 0, completed: 0, playing: 0, avgScore: 0 };

  badges: Badge[] = [];
  chartData: ChartBar[] = [];
  favoriteGame: any = null; 

  destroy$ = new Subject<void>();

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
        const idParam = params.get('id');
        
        if (idParam) {
            this.userId = Number(idParam);
            
            const myId = localStorage.getItem('userId');
            this.isMyProfile = !!myId && Number(myId) === this.userId;
            
            this.userGames = []; 
            this.stats = { total: 0, completed: 0, playing: 0, avgScore: 0 };
            this.badges = [];
            this.favoriteGame = null;
            this.userName = 'Carregando...';
            this.userBio = '';
            this.userAvatar = '';

            this.carregarPerfil();
        }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregarPerfil() {
    this.userService.getById(this.userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.userName = user.name;
          this.userBio = user.bio;
          this.userAvatar = user.profilePicture;
          this.cdr.detectChanges(); 
        },
        error: (err) => {
          console.error('Erro ao buscar detalhes do usuário:', err);
          if (this.userName === 'Carregando...') {
          }
        }
      });

    this.communityService.getUserList(this.userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dados: any) => {
          this.userGames = dados;
          
          if (this.userGames.length > 0) {
             if (this.userName === 'Carregando...' || this.userName === 'Usuário não encontrado') {
                 this.userName = this.userGames[0].user.name;
             }
             this.calcularHallDaFama();
          } 
          
          this.cdr.detectChanges();
        }
      });

    this.communityService.getUserStats(this.userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: any) => {
          this.statsCounts = data;
          this.cdr.detectChanges();
        }
      });
  }

  abrirEdicao() {
    this.editData = {
      name: this.userName || '', 
      bio: this.userBio || '',   
      profilePicture: this.userAvatar || ''
    };
    this.isEditingProfile = true;
  }

  salvarPerfil() {
    this.userService.updateProfile(this.editData).subscribe({
      next: (userAtualizado: any) => {
        this.userName = userAtualizado.name;
        this.userBio = userAtualizado.bio;
        this.userAvatar = userAtualizado.profilePicture;
        
        this.isEditingProfile = false;
        this.cdr.detectChanges(); 
        alert('Perfil atualizado!');
      },
      error: (err) => {
        console.error(err);
        if (err.status === 403) {
            alert('Sem permissão para editar. Verifique se está logado.');
        } else {
            alert('Erro ao salvar. Tente novamente.');
        }
      }
    });
  }

  onListUpdated() {
    this.favoriteGame = null; 
    setTimeout(() => {
       this.communityService.getUserList(this.userId).subscribe(dados => {
          this.userGames = dados;
          this.calcularHallDaFama();
          this.cdr.detectChanges();
       });
    }, 0);
  }

  calcularHallDaFama() {
    if (!this.userGames) return;

    this.stats.total = this.userGames.length;
    this.stats.completed = this.userGames.filter(g => g.status === 'COMPLETED').length;
    this.stats.playing = this.userGames.filter(g => g.status === 'PLAYING').length;

    const ratedGames = this.userGames.filter(g => g.score > 0);
    const counts = new Array(11).fill(0); 
    let maxCount = 0;

    for (const game of ratedGames) {
        counts[game.score]++;
        if (counts[game.score] > maxCount) maxCount = counts[game.score];
    }

    this.chartData = [];
    for (let i = 1; i <= 10; i++) {
        const percentage = maxCount > 0 ? (counts[i] / maxCount * 100) : 0;
        this.chartData.push({ score: i, count: counts[i], heightPc: percentage });
    }

    const totalScore = ratedGames.reduce((sum, g) => sum + g.score, 0);
    this.stats.avgScore = ratedGames.length ? (totalScore / ratedGames.length) : 0;
    
    const manualFavorite = this.userGames.find(g => g.favorite === true);
    this.favoriteGame = manualFavorite ? manualFavorite : (ratedGames.length > 0 ? ratedGames.sort((a, b) => b.score - a.score)[0] : null);

    this.definirBadges(ratedGames);
  }

  definirBadges(ratedGames: any[]) {
    this.badges = [];
    if (this.stats.total >= 1) this.badges.push({ icon: '🎲', label: 'Iniciante', color: '#cd7f32', description: 'Iniciou a coleção.' });
    if (this.stats.total >= 10) this.badges.push({ icon: '📚', label: 'Bibliotecário', color: 'silver', description: '+10 jogos.' });
    if (this.stats.completed >= 5) this.badges.push({ icon: '🏆', label: 'Zerador', color: '#4caf50', description: 'Zerou 5 jogos.' });
    if (this.stats.avgScore >= 9 && ratedGames.length >= 3) this.badges.push({ icon: '⭐', label: 'Bom Gosto', color: '#ffbf00', description: 'Média alta.' });
  }

  getStatusColor(status: string): string {
    const colors: any = { 'PLAYING': '#4caf50', 'COMPLETED': '#2196f3', 'DROPPED': '#f44336', 'PLAN_TO_PLAY': '#9e9e9e' };
    return colors[status] || '#000';
  }
}