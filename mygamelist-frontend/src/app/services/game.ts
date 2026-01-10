import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080';

  // Helper para criar o cabeçalho com o Token
  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`
      })
    };
  }

  // Busca pública (não precisa de token, mas funciona com ele também)
  searchGames(query: string, page: number = 1) { // Padrão 1
    // Note o &page=${page} na URL
    return this.http.get<any[]>(`${this.apiUrl}/games/search?query=${query}&page=${page}`);
  }

  // Adicionar à minha lista (Protegido)
  addGameToList(gameData: any) {
    return this.http.post(`${this.apiUrl}/my-games`, gameData, this.getHeaders());
  }

  // Ver minha lista (Protegido)
  getMyList() {
    return this.http.get<any[]>(`${this.apiUrl}/my-games`, this.getHeaders());
  }

  deleteGame(listId: number) {
    return this.http.delete(`${this.apiUrl}/my-games/${listId}`, this.getHeaders());
  }
}