import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  //private apiUrl = environment.apiUrl;
  private apiUrl = "https://mygamelist-api-65ts.onrender.com";

  // Busca dados públicos de um usuário (Nome, Bio, Foto)
  getById(id: number) {
    return this.http.get<any>(`${this.apiUrl}/users/${id}`);
  }

  // Atualiza o perfil do usuário LOGADO
  updateProfile(data: any) {
    return this.http.put(`${this.apiUrl}/users/me`, data);
  }
}