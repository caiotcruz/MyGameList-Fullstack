import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment'; 

export interface GameReview {
  reviewId: number;
  userName: string;
  userId: number;
  userAvatar: string;
  score: number;
  review: string;
  isSpoiler: boolean;
  showSpoilerText: boolean;
  date: string;
  likesCount: number;
  dislikesCount: number;
  voteScore: number;
  myVote: string | null;
}

export interface GameHubData {
  internalId: number;
  externalId: number;
  title: string;
  coverUrl: string;
  metacritic: number | null;
  totalPlayers: number;
  playingCount: number;
  completedCount: number;
  platinumCount: number;
  communityScore: number;
  userStatus: string | null;
  userScore: number;
  isFavorite: boolean;
  latestReviews: GameReview[];
  scoreDistribution?: { [key: string]: number };
}

export interface TrendingGame {
  id: number;
  rawgId: number;
  title: string;
  coverUrl: string;
  interactionsThisMonth: number;
}

export interface RelatedGame {
  rawgId: number;
  name: string;
  backgroundImage: string;
  released: string;
  metacritic: number | null;
}

export interface GameScreenshot {
  id: number;
  image: string;
}

export interface GameStore {
  storeId: number;
  storeName: string;
  url: string;
}

export interface GameAchievement {
  id: number;
  name: string;
  description: string;
  image: string;
  percent: number;
}

export interface GameTrailer {
  id: number;
  name: string;
  previewImage: string;
  videoUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private http = inject(HttpClient);
  //private apiUrl = environment.apiUrl;
  private apiUrl = "https://mygamelist-api-65ts.onrender.com";

  private getHeaders() {
    const token = localStorage.getItem('token');
    if (!token) return {};
    return {
      headers: new HttpHeaders({ 'Authorization': `Bearer ${token}` })
    };
  }

  searchGames(query: string, page: number = 1) { 
    return this.http.get<any[]>(`${this.apiUrl}/games/search?query=${query}&page=${page}`);
  }

  getGameHub(rawgId: string) {
    return this.http.get<GameHubData>(`${this.apiUrl}/games/${rawgId}/hub`, this.getHeaders());
  }

  addGameToList(gameData: any) {
    return this.http.post(`${this.apiUrl}/my-games`, gameData, this.getHeaders());
  }

  getMyList() {
    return this.http.get<any[]>(`${this.apiUrl}/my-games`, this.getHeaders());
  }

  deleteGame(listId: number) {
    return this.http.delete(`${this.apiUrl}/my-games/${listId}`, this.getHeaders());
  }

  voteReview(reviewId: number, type: 'LIKE' | 'DISLIKE') {
    return this.http.post(`${this.apiUrl}/reviews/${reviewId}/vote`, { type }, this.getHeaders());
  }

  getTrendingGames(limit: number = 10) {
    return this.http.get<TrendingGame[]>(`${this.apiUrl}/games/trending?limit=${limit}`, this.getHeaders());
  }

  getAdditions(rawgId: string) {
    return this.http.get<RelatedGame[]>(`${this.apiUrl}/games/${rawgId}/additions`, this.getHeaders());
  }

  getGameSeries(rawgId: string) {
    return this.http.get<RelatedGame[]>(`${this.apiUrl}/games/${rawgId}/series`, this.getHeaders());
  }

  getStores(rawgId: string) {
    return this.http.get<GameStore[]>(`${this.apiUrl}/games/${rawgId}/stores`, this.getHeaders());
  }

  getAchievements(rawgId: string) {
    return this.http.get<GameAchievement[]>(`${this.apiUrl}/games/${rawgId}/achievements`, this.getHeaders());
  }

  getScreenshots(rawgId: string) {
    return this.http.get<GameScreenshot[]>(`${this.apiUrl}/games/${rawgId}/screenshots`, this.getHeaders());
  }

  getTrailers(rawgId: string) {
    return this.http.get<GameTrailer[]>(`${this.apiUrl}/games/${rawgId}/trailers`, this.getHeaders());
  }
}