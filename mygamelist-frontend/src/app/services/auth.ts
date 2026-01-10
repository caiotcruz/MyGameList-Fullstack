import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // No Angular 17+, usamos 'inject' em vez de construtor
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/auth';

  constructor() { }

  login(credentials: any) {
    return this.http.post(this.apiUrl + '/login', credentials, { responseType: 'text' })
      .pipe(
        tap((token) => {
          // Salva o token no navegador quando o login der certo
          localStorage.setItem('token', token);
        })
      );
  }

  logout() {
    localStorage.removeItem('token');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }
}