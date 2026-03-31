import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, Subject, takeUntil } from 'rxjs';

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
  // Injeções
  private route = inject(ActivatedRoute);
  private communityService = inject(CommunityService);
  private userService = inject(UserService); 
  private cdr = inject(ChangeDetectorRef);

  // Estados
  userId: number = 0;
  userGames: any[] = [];
  userName: string = 'Carregando...';
  userBio: string = '';  
  userAvatar: string = ''; 
  isMyProfile: boolean = false;
  isEditingProfile = false; 
  editData = { name: '', bio: '', profilePicture: '' };

  // Inicialização segura para evitar "undefined" no HTML
  statsCounts = { following: 0, followers: 0 };
  stats = { total: 0, completed: 0, playing: 0, avgScore: 0 };
  badges: Badge[] = [];
  chartData: ChartBar[] = [];
  favoriteGame: any = null; 

  private destroy$ = new Subject<void>();

  ngOnInit() {
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
    const myId = localStorage.getItem('userId');
    this.isMyProfile = !!myId && Number(myId) === this.userId;
  }

  carregarPerfil() {
    // Usamos forkJoin para evitar múltiplos ciclos de renderização (opcional, mas recomendado)s
    forkJoin({
      user: this.userService.getById(this.userId),
      games: this.communityService.getUserList(this.userId),
      stats: this.communityService.getUserStats(this.userId)
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (res: any) => {
        this.userName = res.user.name;
        this.userBio = res.user.bio;
        this.userAvatar = res.user.profilePicture;
        this.userGames = res.games;
        this.statsCounts = res.stats;

        this.calcularHallDaFama();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar perfil completo:', err);
        this.userName = 'Usuário não encontrado';
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
      },
      error: (err) => alert('Erro ao salvar perfil.')
    });
  }

  onListUpdated() {
    // Ao atualizar a lista, buscamos apenas os jogos e as estatísticas sociais
    this.communityService.getUserList(this.userId).subscribe(dados => {
      this.userGames = dados;
      this.calcularHallDaFama();
      this.cdr.detectChanges();
    });
  }

  calcularHallDaFama() {
    if (!this.userGames || this.userGames.length === 0) {
      this.favoriteGame = null; // Se não tem jogo, banner fica preto (cor de fundo do CSS)
      return;
    }

    // Cálculos Básicos
    const total = this.userGames.length;
    const completed = this.userGames.filter(g => g.status === 'COMPLETED').length;
    const playing = this.userGames.filter(g => g.status === 'PLAYING').length;
    
    const ratedGames = this.userGames.filter(g => g.score > 0);
    const totalScore = ratedGames.reduce((sum, g) => sum + g.score, 0);
    const avgScore = ratedGames.length ? (totalScore / ratedGames.length) : 0;

    this.stats = { total, completed, playing, avgScore };

    // Lógica do Gráfico
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

    // Lógica de Favorito
    const manualFavorite = this.userGames.find(g => g.favorite === true);
    this.favoriteGame = manualFavorite || 
                       (ratedGames.length > 0 ? [...ratedGames].sort((a, b) => b.score - a.score)[0] : this.userGames[0]);

    this.definirBadges(ratedGames);
  }

  private definirBadges(ratedGames: any[]) {
    const b: Badge[] = [];
    if (this.stats.total >= 1) b.push({ icon: '🎲', label: 'Iniciante', color: '#cd7f32', description: 'Iniciou a coleção.' });
    if (this.stats.total >= 10) b.push({ icon: '📚', label: 'Bibliotecário', color: 'silver', description: '+10 jogos na conta.' });
    if (this.stats.completed >= 5) b.push({ icon: '🏆', label: 'Zerador', color: '#4caf50', description: 'Completou 5 desafios.' });
    if (this.stats.avgScore >= 9 && ratedGames.length >= 3) b.push({ icon: '⭐', label: 'Sommelier', color: '#ffbf00', description: 'Média de notas excelente.' });
    this.badges = b;
  }

  // TrackBys para performance
  trackByScore(index: number, item: ChartBar) { return item.score; }
  trackByBadge(index: number, item: Badge) { return item.label; }
}