import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { DatePipe, CommonModule } from '@angular/common';
import { ActivityService } from '../../services/activity'; 
import { Activity } from '../../models/activity.model';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './feed.html',
  styleUrl: './feed.css'
})
export class Feed implements OnInit {
  activityService = inject(ActivityService);
  cdr = inject(ChangeDetectorRef); 

  activities: Activity[] = [];

  ngOnInit() {
    this.activityService.getFeed().subscribe({
      next: (data) => {
        this.activities = data;
        this.cdr.detectChanges(); 
      },
      error: (err) => console.error('Erro ao carregar feed', err)
    });
  }

  getActionText(type: string): string {
    switch(type) {
      case 'ADDED_TO_LIST': return 'adicionou à biblioteca';
      case 'CHANGED_STATUS': return 'alterou o status para';
      case 'RATED': return 'deu nota';
      case 'REVIEWED': return 'fez uma review de';
      default: return 'atualizou';
    }
  }
  
  formatDetail(type: string, detail: string): string {
    if (type === 'RATED') return `⭐ ${detail}/10`;
    if (type === 'CHANGED_STATUS') return detail?.replace('_', ' '); 
    return detail || '';
  }
}