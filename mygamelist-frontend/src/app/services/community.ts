import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment'; 

@Injectable({
  providedIn: 'root'
})
export class CommunityService {
  private http = inject(HttpClient);
 private apiUrl = "https://mygamelist-api-65ts.onrender.com/community";
 //private apiUrl = environment.apiUrl + '/community';

  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`
      })
    };
  }

  getAllUsers() {
    return this.http.get<any[]>(`${this.apiUrl}/users`, this.getHeaders());
  }

  getUserList(userId: number) {
    return this.http.get<any[]>(`${this.apiUrl}/users/${userId}/list`, this.getHeaders());
  }

  toggleLike(activityId: number) {
    return this.http.post<boolean>(`${this.apiUrl}/activities/${activityId}/like`, {});
  }

  postComment(activityId: number, text: string) {
    return this.http.post<any>(`${this.apiUrl}/activities/${activityId}/comments`, { text });
  }
}