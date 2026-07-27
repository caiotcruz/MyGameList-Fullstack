import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FollowService {
  private http = inject(HttpClient);

  private apiUrl = "https://mygamelist-api-65ts.onrender.com/users";
  //private apiUrl = 'http://localhost:8080/users'; 

  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`
      })
    };
  }

  follow(userId: number) {
    return this.http.post(`${this.apiUrl}/${userId}/follow`, {});
  }

  unfollow(userId: number) {
    return this.http.delete(`${this.apiUrl}/${userId}/unfollow`);
  }

  isFollowing(userId: number) {
    return this.http.get<boolean>(`${this.apiUrl}/${userId}/is-following`);
  }
}