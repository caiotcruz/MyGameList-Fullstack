import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';

interface LoginResponse {
  token: string;
  userId: number;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl ="https://mygamelist-api-65ts.onrender.com/auth";
  //private apiUrl = environment.apiUrl + '/auth';

  constructor() { }

  login(credentials: any) {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/login`, credentials);
  }

  register(userData: any) {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/register`, userData);
  }

  verify(data: { email: string, codigo: string }) {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/verify`, data);
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.clear();
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }
}