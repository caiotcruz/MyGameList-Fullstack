import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  http = inject(HttpClient);
  private apiUrl = "https://mygamelist-api-65ts.onrender.com";
  //private apiUrl = environment.apiUrl;

  getNotifications() {
    return this.http.get<any[]>(`${this.apiUrl}/notifications`);
  }

  getUnreadCount() {
    return this.http.get<number>(`${this.apiUrl}/notifications/unread-count`);
  }

  markAsRead(id: number) {
    return this.http.put(`${this.apiUrl}/notifications/${id}/read`, {});
  }
  
  markAllAsRead() {
    return this.http.put(`${this.apiUrl}/notifications/read-all`, {});
  }
}