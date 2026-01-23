import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment'; 

export interface GameReview {
  userName: string;
  userAvatar: string;
  score: number;
  review: string;
  date: string;
}

export interface GameHubData {
  internalId: number;
  externalId: number;
  title: string;
  coverUrl: string;
  totalPlayers: number;
  playingCount: number;
  completedCount: number;
  communityScore: number;
  userStatus: string | null;
  userScore: number;
  isFavorite: boolean;
  latestReviews: GameReview[];
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
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`
      })
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
}