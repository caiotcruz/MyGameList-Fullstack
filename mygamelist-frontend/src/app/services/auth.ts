import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // No Angular 17+, usamos 'inject' em vez de construtor
  private http = inject(HttpClient);

  private apiUrl = 'https://mygamelist-api-65ts.onrender.com/auth';

  constructor() { }

  login(credentials: any) {
    return this.http.post(this.apiUrl + '/login', credentials, { responseType: 'text' })
      .pipe(
        tap((token) => {
          localStorage.setItem('token', token);
        })
      );
  }

  register(userData: any) {
    return this.http.post(this.apiUrl + '/register', userData);
  }

  logout() {
    localStorage.removeItem('token');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }
}