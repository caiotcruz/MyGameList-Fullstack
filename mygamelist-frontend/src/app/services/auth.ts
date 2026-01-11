import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);

  private apiUrl = environment.apiUrl + '/auth';

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