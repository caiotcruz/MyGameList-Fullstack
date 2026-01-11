import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommunityService } from '../../services/community';
import { FollowService } from '../../services/follow'; 
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-community',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './community.html',
  styleUrl: './community.css'
})
export class Community implements OnInit {
  communityService = inject(CommunityService);
  followService = inject(FollowService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);
  
  users: any[] = [];
  isLoading = false;

  ngOnInit() {
    this.isLoading = true;
    this.communityService.getAllUsers().subscribe({
      next: (dados) => {
        this.users = dados;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => { 
        console.error(err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  verPerfil(userId: number) {
    this.router.navigate(['/profile', userId]);
  }

  toggleFollow(user: any, event: Event) {
    event.stopPropagation(); 
    
    if (user.isFollowing) {
      this.followService.unfollow(user.id).subscribe({
        next: () => {
          user.isFollowing = false; 
          this.cdr.detectChanges();
        },
        error: () => alert('Erro ao deixar de seguir.')
      });
    } else {
      this.followService.follow(user.id).subscribe({
        next: () => {
          user.isFollowing = true; 
          this.cdr.detectChanges();
        },
        error: () => alert('Erro ao seguir.')
      });
    }
  }
}