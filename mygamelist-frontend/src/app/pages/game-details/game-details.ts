import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  GameService, GameHubData, RelatedGame, GameStore,
  GameAchievement, GameScreenshot, GameTrailer
} from '../../services/game';

interface ChartBar {
  score: number;
  count: number;
  heightPc: number;
}

@Component({
  selector: 'app-game-details',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './game-details.html',
  styleUrl: './game-details.css'
})
export class GameDetails implements OnInit {
  route = inject(ActivatedRoute);
  gameService = inject(GameService);
  cdr = inject(ChangeDetectorRef);

  data: GameHubData | null = null;
  isLoading = true;
  chartData: ChartBar[] = [];

  isModalOpen = false;
  isSaving = false;
  editingGame: any = {};

  additions: RelatedGame[] = [];
  series: RelatedGame[] = [];
  stores: GameStore[] = [];
  achievements: GameAchievement[] = [];
  screenshots: GameScreenshot[] = [];
  trailers: GameTrailer[] = [];

  showAllAchievements = false;
  lightboxIndex: number | null = null;
  activeTrailer: GameTrailer | null = null;

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) this.carregarHub(id);
    });
  }

  carregarHub(id: string) {
    this.isLoading = true;
    this.resetExtras();

    this.gameService.getGameHub(id).subscribe({
      next: (res) => {
        if (res.latestReviews) {
          res.latestReviews = res.latestReviews.map((r: any) => ({
            ...r,
            showSpoilerText: false
          }));
        }
        this.data = res;
        this.processarGraficoDeNotas(res.scoreDistribution);
        this.isLoading = false;
        this.cdr.detectChanges();
        this.carregarExtras(id);
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  private resetExtras() {
    this.additions = [];
    this.series = [];
    this.stores = [];
    this.achievements = [];
    this.screenshots = [];
    this.trailers = [];
    this.showAllAchievements = false;
    this.lightboxIndex = null;
    this.activeTrailer = null;
  }

  private carregarExtras(rawgId: string) {
    forkJoin({
      additions: this.gameService.getAdditions(rawgId),
      series: this.gameService.getGameSeries(rawgId),
      stores: this.gameService.getStores(rawgId),
      achievements: this.gameService.getAchievements(rawgId)
    }).subscribe({
      next: ({ additions, series, stores, achievements }) => {
        this.additions = additions;
        this.series = series;
        this.stores = stores;
        this.achievements = [...achievements].sort((a, b) => a.percent - b.percent);
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erro ao carregar dados extras da RAWG:', err)
    });

    this.gameService.getScreenshots(rawgId).subscribe({
      next: (res) => {
        this.screenshots = res;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erro ao carregar screenshots:', err)
    });

    this.gameService.getTrailers(rawgId).subscribe({
      next: (res) => {
        this.trailers = res;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erro ao carregar trailers:', err)
    });
  }

  get visibleAchievements(): GameAchievement[] {
    return this.showAllAchievements ? this.achievements : this.achievements.slice(0, 6);
  }

  toggleShowAchievements() {
    this.showAllAchievements = !this.showAllAchievements;
  }

  rarityClass(percent: number): string {
    if (percent < 10) return 'rarity-legendary';
    if (percent < 40) return 'rarity-rare';
    return 'rarity-common';
  }

  metacriticClass(score: number | null): string {
    if (score == null) return '';
    if (score >= 75) return 'meta-high';
    if (score >= 50) return 'meta-mid';
    return 'meta-low';
  }

  openLightbox(index: number) {
    this.lightboxIndex = index;
  }

  closeLightbox() {
    this.lightboxIndex = null;
  }

  nextScreenshot(event: MouseEvent) {
    event.stopPropagation();
    if (this.lightboxIndex === null) return;
    this.lightboxIndex = (this.lightboxIndex + 1) % this.screenshots.length;
  }

  prevScreenshot(event: MouseEvent) {
    event.stopPropagation();
    if (this.lightboxIndex === null) return;
    this.lightboxIndex = (this.lightboxIndex - 1 + this.screenshots.length) % this.screenshots.length;
  }

  playTrailer(trailer: GameTrailer) {
    this.activeTrailer = trailer;
  }

  closeTrailer() {
    this.activeTrailer = null;
  }

  toggleSpoiler(rev: any) {
    rev.showSpoilerText = !rev.showSpoilerText;
    this.cdr.detectChanges();
  }

  processarGraficoDeNotas(distributionMap: { [key: string]: number } | undefined) {
    if (!distributionMap) {
      this.chartData = [];
      return;
    }

    let maxCount = 0;
    Object.values(distributionMap).forEach(count => {
      if (count > maxCount) maxCount = count;
    });

    this.chartData = Array.from({ length: 10 }, (_, i) => {
      const score = i + 1;
      const count = distributionMap[score.toString()] || distributionMap[score] || 0;
      return {
        score,
        count,
        heightPc: maxCount > 0 ? (count / maxCount) * 100 : 0
      };
    });
  }

  trackByScore(index: number, item: ChartBar) {
    return item.score;
  }

  abrirModal() {
    if (!this.data) return;

    const userId = localStorage.getItem('userId');
    let myExistingReview = '';
    let myExistingIsSpoiler = false;

    if (userId && this.data.latestReviews) {
      const userReview = this.data.latestReviews.find((r: any) => r.myVote !== undefined);
      if (userReview) {
        myExistingReview = userReview.review;
        myExistingIsSpoiler = userReview.isSpoiler || false;
      }
    }

    this.editingGame = {
      rawgId: this.data.externalId,
      title: this.data.title,
      coverUrl: this.data.coverUrl,
      status: this.data.userStatus || 'PLAN_TO_PLAY',
      score: this.data.userScore || 0,
      review: myExistingReview,
      isSpoiler: myExistingIsSpoiler
    };

    this.isModalOpen = true;
    this.cdr.detectChanges();
  }

  fecharModal() {
    this.isModalOpen = false;
  }

  verificarStatus() {
    if (this.editingGame.status === 'PLAN_TO_PLAY') {
      this.editingGame.score = 0;
      this.editingGame.review = '';
      this.editingGame.isSpoiler = false;
    }
  }

  validarScore() {
    this.editingGame.score = Math.min(10, Math.max(0, Math.floor(this.editingGame.score || 0)));
  }

  salvar() {
    if (this.isSaving) return;
    this.validarScore();
    this.isSaving = true;

    this.gameService.addGameToList(this.editingGame).subscribe({
      next: () => {
        this.isSaving = false;
        this.fecharModal();
        if (this.data) this.carregarHub(this.data.externalId.toString());
      },
      error: (err) => {
        alert('Erro ao salvar. Verifique se você está logado.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  getScoreColor(score: number): string {
    if (score >= 9) return '#4caf50';
    if (score >= 7) return '#2196f3';
    if (score >= 5) return '#ff9800';
    return '#f44336';
  }

  getStatusLabel(status: string): string {
    const map: any = {
      'PLAYING': 'Jogando',
      'COMPLETED': 'Zerado',
      'PLATINUM': 'Platinado',
      'PLAN_TO_PLAY': 'Quero Jogar',
      'DROPPED': 'Larguei'
    };
    return map[status] || status;
  }

  votar(rev: any, type: 'LIKE' | 'DISLIKE') {
    const reviewId = rev.reviewId || rev.id;

    if (!reviewId) {
      console.error("❌ ERRO CRÍTICO: ID da review veio nulo/undefined!", rev);
      return;
    }

    if (rev.myVote === type) {
      rev.myVote = null;
      if (type === 'LIKE') rev.likesCount--;
      else rev.dislikesCount--;
    } else {
      if (rev.myVote === 'LIKE') rev.likesCount--;
      if (rev.myVote === 'DISLIKE') rev.dislikesCount--;

      rev.myVote = type;
      if (type === 'LIKE') rev.likesCount++;
      else rev.dislikesCount++;
    }

    rev.voteScore = (rev.likesCount * 2) - rev.dislikesCount;

    this.gameService.voteReview(reviewId, type).subscribe({
      error: (err) => {
        console.error('Erro no voto:', err);
      }
    });
  }
}