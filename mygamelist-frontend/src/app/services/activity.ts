import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { GroupedActivity } from '../models/activity.model';

export interface FeedPage { items: GroupedActivity[]; nextCursor: number | null; hasMore: boolean; }

@Injectable({
  providedIn: 'root'
})
export class ActivityService {
  private http = inject(HttpClient);
  //private apiUrl = "https://mygamelist-api-65ts.onrender.com/community/activities";
  private apiUrl = environment.apiUrl + '/community/activities';

  getFeed(before: number | null = null) {
    return this.http.get<FeedPage>(this.apiUrl, before === null ? {} : { params: { before } });
  }
}