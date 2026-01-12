import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Activity } from '../models/activity.model';

@Injectable({
  providedIn: 'root'
})
export class ActivityService {
  private http = inject(HttpClient);
  private apiUrl = "https://mygamelist-api-65ts.onrender.com/activities";
  //private apiUrl = environment.apiUrl + '/activities';

  getFeed() {
    return this.http.get<Activity[]>(this.apiUrl);
  }
}