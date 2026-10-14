import { TEXT_LIMITS } from '../../shared/text-limits';
import { ThemeService, THEMES, ThemeId } from '../../services/theme';
import { SafetyActions } from '../../components/safety-actions/safety-actions';
import { Feedback, errorMessage } from '../../services/feedback';
import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, Subject, takeUntil } from 'rxjs';

import { CommunityService, Achievement } from '../../services/community';
import { FollowService } from '../../services/follow';
import { GameList } from '../../components/game-list/game-list';
import { UserService } from '../../services/user';

interface ChartBar {
  score: number;
  count: number;
  heightPc: number;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [SafetyActions, CommonModule, GameList, FormsModule, RouterModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit, OnDestroy {
  readonly limits = TEXT_LIMITS;
  theme = inject(ThemeService);
  readonly themes = THEMES;

  feedback=inject(Feedback);

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
  editData = { name: '', bio: '', profilePicture: '', rotatingAvatar: false, theme: 'TEAL' as ThemeId };

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
  achievements: Achievement[] = [];
  showAllAchievements = false;
  readonly achievementPreviewLimit=3;
  get visibleAchievements():Achievement[]{return this.achievements.slice(0,this.achievementPreviewLimit);}
  profileError='';
  onBlocked(){this.router.navigate(['/privacy']);}
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
    this.profileError="";
    const requests: any = {
      user: this.userService.getById(this.userId),
      games: this.communityService.getUserList(this.userId),
      stats: this.communityService.getUserStats(this.userId),
      achievements: this.communityService.getAchievements(this.userId)
    };

    if (!this.isMyProfile && this.myId > 0) {
      requests.myStats = this.communityService.getUserStats(this.myId);
    }

    const requestedUserId=this.userId;
    forkJoin(requests)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          if(requestedUserId!==this.userId)return;
          this.userName = res.user.name;
          this.userBio = res.user.bio;
          this.userAvatar = res.user.profilePicture;
          this.isRotatingAvatar = res.user.rotatingAvatar;
          this.userLevel = res.user.level ?? 1;
          this.userExperience = res.user.experience ?? 0;
          this.userGames = res.games;
          this.statsData = res.stats;
          this.achievements = res.achievements;
          this.showAllAchievements = false;

          if (this.isMyProfile) {
            this.myFollowingIds = new Set(res.stats.following?.map((u: any) => u.id) || []);
          } else if (res.myStats) {
            this.myFollowingIds = new Set(res.myStats.following?.map((u: any) => u.id) || []);
          }

          this.calcularHallDaFama();
          this.cdr.detectChanges();
        },
        error: (err) => {
          if(requestedUserId!==this.userId)return;
          this.profileError=errorMessage(err);
          this.userName='Perfil indisponível';
          this.userGames=[];this.achievements=[];this.favoriteGame=null;
          this.cdr.detectChanges();
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

  pendingFollowIds = new Set<number>();

  toggleFollowUser(targetUser: any, event: Event) {
    event.stopPropagation();
    if (this.pendingFollowIds.has(targetUser.id)) return;
    this.pendingFollowIds.add(targetUser.id);
    const request = this.euSigo(targetUser.id)
      ? this.followService.unfollow(targetUser.id)
      : this.followService.follow(targetUser.id);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.pendingFollowIds.delete(targetUser.id);
        this.carregarPerfil();
      },
      error: (err) => {
        this.pendingFollowIds.delete(targetUser.id);
        this.feedback.error(err);
        this.cdr.detectChanges();
      }
    });
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
      rotatingAvatar: this.isRotatingAvatar || false,
      theme: this.theme.selected()
    };
    this.isEditingProfile = true;
  }

  salvarPerfil() {
    if ((this.editData.bio?.length || 0) > this.limits.BIO || (this.editData.name?.length || 0) > this.limits.NAME) {
      this.feedback.show('Confira os limites de nome e biografia. Seu texto foi preservado.'); return;
    }
    this.userService.updateProfile(this.editData).subscribe({
      next: (userAtualizado: any) => {
        this.userName = userAtualizado.name;
        this.userBio = userAtualizado.bio;
        this.userAvatar = userAtualizado.profilePicture;
        this.isRotatingAvatar = userAtualizado.rotatingAvatar;
        this.theme.setSaved(userAtualizado.theme);
        this.isEditingProfile = false;
        this.cdr.detectChanges();
      },
      error: (err) => this.feedback.error(err)
    });
  }

  onListUpdated() {
    this.carregarPerfil();
  }

  calcularHallDaFama() {
    if (!this.userGames || this.userGames.length === 0) {
      this.favoriteGame = null;
      this.stats={total:0,completed:0,playing:0,platinum:0,avgScore:0};this.chartData=[];
      return;
    }

    const total = this.userGames.length;
    const completed = this.userGames.filter(g => g.status === 'COMPLETED' || g.status === 'PLATINUM').length;
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


  }

  trackByScore(index: number, item: ChartBar) { return item.score; }
}
