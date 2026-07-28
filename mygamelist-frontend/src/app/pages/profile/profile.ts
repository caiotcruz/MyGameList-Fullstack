import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, Subject, takeUntil } from 'rxjs';

import { CommunityService } from '../../services/community';
import { FollowService } from '../../services/follow';
import { GameList } from '../../components/game-list/game-list'; 
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
  private route = inject(ActivatedRoute);
  private communityService = inject(CommunityService);
  private followService = inject(FollowService);
  private userService = inject(UserService); 
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  userId: number = 0;
  myId: number = 0;
  userGames: any[] = [];
  userName: string = 'Carregando...';
  userBio: string = '';   
  userAvatar: string = '';
  userLevel: number = 1;
  userExperience: number = 0;
  readonly xpPerLevel: number = 1000;

  isRotatingAvatar: boolean = false; 
  isMyProfile: boolean = false;
  isEditingProfile = false; 
  editData = { name: '', bio: '', profilePicture: '', rotatingAvatar: false };

  statsData: {
    followersCount: number;
    followingCount: number;
    followers: any[];
    following: any[];
  } = { followersCount: 0, followingCount: 0, followers: [], following: [] };

  isFollowModalOpen = false;
  activeFollowTab: 'followers' | 'following' = 'followers';
  myFollowingIds: Set<number> = new Set();

  stats = { total: 0, completed: 0, playing: 0, platinum: 0, avgScore: 0 };
  badges: Badge[] = [];
  chartData: ChartBar[] = [];
  favoriteGame: any = null; 

  private destroy$ = new Subject<void>();

  ngOnInit() {
    this.myId = Number(localStorage.getItem('userId') || 0);

    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        this.userId = Number(idParam);
        this.verificarPropriedade();
        this.carregarPerfil();
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private verificarPropriedade() {
    this.isMyProfile = !!this.myId && this.myId === this.userId;
  }

  carregarPerfil() {
    const requests: any = {
      user: this.userService.getById(this.userId),
      games: this.communityService.getUserList(this.userId),
      stats: this.communityService.getUserStats(this.userId)
    };

    if (!this.isMyProfile && this.myId > 0) {
      requests.myStats = this.communityService.getUserStats(this.myId);
    }

    forkJoin(requests)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.userName = res.user.name;
          this.userBio = res.user.bio;
          this.userAvatar = res.user.profilePicture;
          this.isRotatingAvatar = res.user.rotatingAvatar; 
          this.userLevel = res.user.level ?? 1;
          this.userExperience = res.user.experience ?? 0;
          this.userGames = res.games;
          this.statsData = res.stats;

          if (this.isMyProfile) {
            this.myFollowingIds = new Set(res.stats.following?.map((u: any) => u.id) || []);
          } else if (res.myStats) {
            this.myFollowingIds = new Set(res.myStats.following?.map((u: any) => u.id) || []);
          }

          this.calcularHallDaFama();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Erro ao carregar perfil completo:', err);
          this.userName = 'Usuário não encontrado';
        }
      });
  }

  get xpPercentage(): number {
    return Math.min(100, Math.max(0, (this.userExperience / this.xpPerLevel) * 100));
  }

  abrirFollowModal(tab: 'followers' | 'following') {
    this.activeFollowTab = tab;
    this.isFollowModalOpen = true;
    this.cdr.detectChanges();
  }

  fecharFollowModal() {
    this.isFollowModalOpen = false;
  }

  euSigo(targetUserId: number): boolean {
    return this.myFollowingIds.has(targetUserId);
  }

  toggleFollowUser(targetUser: any, event: Event) {
    event.stopPropagation();
    const isFollowing = this.euSigo(targetUser.id);

    if (isFollowing) {
      this.myFollowingIds.delete(targetUser.id);
      if (this.isMyProfile) {
        this.statsData.followingCount--;
        this.statsData.following = this.statsData.following.filter(u => u.id !== targetUser.id);
      }
      this.cdr.detectChanges();

      this.followService.unfollow(targetUser.id).subscribe({
        error: () => {
          this.myFollowingIds.add(targetUser.id);
          this.cdr.detectChanges();
          alert('Erro ao deixar de seguir.');
        }
      });
    } else {
      this.myFollowingIds.add(targetUser.id);
      if (this.isMyProfile) {
        this.statsData.followingCount++;
        this.statsData.following.push(targetUser);
      }
      this.cdr.detectChanges();

      this.followService.follow(targetUser.id).subscribe({
        error: () => {
          this.myFollowingIds.delete(targetUser.id);
          this.cdr.detectChanges();
          alert('Erro ao seguir.');
        }
      });
    }
  }

  navegarParaPerfil(targetUserId: number) {
    this.fecharFollowModal();
    this.router.navigate(['/profile', targetUserId]);
  }

  abrirEdicao() {
    this.editData = {
      name: this.userName || '', 
      bio: this.userBio || '',   
      profilePicture: this.userAvatar || '',
      rotatingAvatar: this.isRotatingAvatar || false
    };
    this.isEditingProfile = true;
  }

  salvarPerfil() {
    this.userService.updateProfile(this.editData).subscribe({
      next: (userAtualizado: any) => {
        this.userName = userAtualizado.name;
        this.userBio = userAtualizado.bio;
        this.userAvatar = userAtualizado.profilePicture;
        this.isRotatingAvatar = userAtualizado.rotatingAvatar;
        this.isEditingProfile = false;
        this.cdr.detectChanges(); 
      },
      error: (err) => alert('Erro ao salvar perfil.')
    });
  }

  onListUpdated() {
    this.communityService.getUserList(this.userId).subscribe(dados => {
      this.userGames = dados;
      this.calcularHallDaFama();
      this.cdr.detectChanges();
    });
  }

  calcularHallDaFama() {
    if (!this.userGames || this.userGames.length === 0) {
      this.favoriteGame = null;
      return;
    }

    const total = this.userGames.length;
    const completed = this.userGames.filter(g => g.status === 'COMPLETED').length;
    const platinum = this.userGames.filter(g => g.status === 'PLATINUM').length;
    const playing = this.userGames.filter(g => g.status === 'PLAYING').length;
    
    const ratedGames = this.userGames.filter(g => g.score > 0);
    const totalScore = ratedGames.reduce((sum, g) => sum + g.score, 0);
    const avgScore = ratedGames.length ? (totalScore / ratedGames.length) : 0;

    this.stats = { total, completed, playing, platinum, avgScore };

    const counts = new Array(11).fill(0); 
    let maxCount = 0;
    ratedGames.forEach(g => {
      counts[g.score]++;
      if (counts[g.score] > maxCount) maxCount = counts[g.score];
    });

    this.chartData = Array.from({length: 10}, (_, i) => {
      const score = i + 1;
      return {
        score,
        count: counts[score],
        heightPc: maxCount > 0 ? (counts[score] / maxCount * 100) : 0
      };
    });

    const manualFavorite = this.userGames.find(g => g.favorite === true);
    this.favoriteGame = manualFavorite || 
                       (ratedGames.length > 0 ? [...ratedGames].sort((a, b) => b.score - a.score)[0] : this.userGames[0]);

    this.definirBadges(ratedGames);
  }

  private definirBadges(ratedGames: any[]) {
    const b: Badge[] = [];
    if (this.stats.total >= 1) b.push({ icon: 'dice', label: 'Iniciante', color: '#cd7f32', description: 'Iniciou a coleção.' });
    if (this.stats.total >= 10) b.push({ icon: 'book', label: 'Bibliotecário', color: 'silver', description: '+10 jogos na conta.' });
    if (this.stats.completed >= 5) b.push({ icon: 'trophy', label: 'Zerador', color: '#4caf50', description: 'Completou 5 desafios.' });
    if (this.stats.avgScore >= 9 && ratedGames.length >= 3) b.push({ icon: 'star', label: 'Sommelier', color: '#ffbf00', description: 'Média de notas excelente.' });
    if (this.stats.platinum >= 1) {
        b.push({ icon: 'gem', label: 'Perfeccionista', color: '#b400ff', description: 'Platinou seu primeiro jogo.' });
    }
    this.badges = b;
  }

  getBadgeIconClass(iconKey: string): string {
    const map: { [key: string]: string } = {
      'dice': 'fa-solid fa-dice',
      'book': 'fa-solid fa-book',
      'trophy': 'fa-solid fa-trophy',
      'star': 'fa-solid fa-star',
      'gem': 'fa-solid fa-gem'
    };
    return map[iconKey] || 'fa-solid fa-medal';
  }

  trackByScore(index: number, item: ChartBar) { return item.score; }
  trackByBadge(index: number, item: Badge) { return item.label; }
}