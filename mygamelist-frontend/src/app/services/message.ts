import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';

export interface ConversationSummary {
  conversationId: number;
  otherUser: { id: number; name: string; profilePicture: string | null };
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  lastMessageFromMe: boolean;
  unreadCount: number;
}

export interface Message {
  id: number;
  senderId: number;
  content: string;
  sentAt: string;
  mine: boolean;
}

export interface MessageableContact {
  id: number;
  name: string;
  profilePicture: string | null;
}

export interface MessageThread {
  partner: { id: number; name: string; profilePicture: string | null };
  messages: Message[];
}

@Injectable({ providedIn: 'root' })
export class MessageService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/messages';

  private getHeaders() {
    const token = localStorage.getItem('token');
    if (!token) return {};
    return { headers: new HttpHeaders({ 'Authorization': `Bearer ${token}` }) };
  }

  getConversations() {
    return this.http.get<ConversationSummary[]>(`${this.apiUrl}/conversations`, this.getHeaders());
  }

  getThread(userId: number) {
    return this.http.get<MessageThread>(`${this.apiUrl}/conversations/${userId}`, this.getHeaders());
  }

  sendMessage(userId: number, content: string) {
    return this.http.post<Message>(`${this.apiUrl}/conversations/${userId}`, { content }, this.getHeaders());
  }

  getUnreadCount() {
    return this.http.get<number>(`${this.apiUrl}/unread-count`, this.getHeaders());
  }

  getContacts() {
    return this.http.get<MessageableContact[]>(`${this.apiUrl}/contacts`, this.getHeaders());
  }
}