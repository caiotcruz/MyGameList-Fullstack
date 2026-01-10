import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CommunityService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/community';

  // Helper de Token (Igual ao do GameService)
  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`
      })
    };
  }

  // Busca todos os usuários
  getAllUsers() {
    return this.http.get<any[]>(`${this.apiUrl}/users`, this.getHeaders());
  }

  // Busca a lista de jogos de um amigo pelo ID dele
  getUserList(userId: number) {
    return this.http.get<any[]>(`${this.apiUrl}/users/${userId}/list`, this.getHeaders());
  }
}