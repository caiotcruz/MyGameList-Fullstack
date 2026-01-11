import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment'; 

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // Helper para criar o cabeçalho com o Token
  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`
      })
    };
  }

  searchGames(query: string, page: number = 1) { 
    return this.http.get<any[]>(`${this.apiUrl}/games/search?query=${query}&page=${page}`);
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