import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommunityService } from '../../services/community';
import { FollowService } from '../../services/follow'; 
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subject, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';

@Component({
  selector: 'app-community',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './community.html',
  styleUrl: './community.css'
})
export class Community implements OnInit {
  communityService = inject(CommunityService);
  followService = inject(FollowService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);
  
  users: any[] = [];
  allUsers: any[] = []; 
  searchQuery = '';
  isLoading = false;

  private searchSubject = new Subject<string>();

  ngOnInit() {
    this.carregarUsuariosIniciais();

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(), 
      switchMap(termo => {
        this.isLoading = true;
        this.cdr.detectChanges();

        if (!termo.trim()) {
          return of(this.allUsers.slice(0, 12));
        }
        
        return this.communityService.getUserListByName(termo).pipe(
          catchError(() => of([])) 
        );
      })
    ).subscribe(dados => {
      this.users = dados;
      this.isLoading = false;
      this.cdr.detectChanges();
    });
  }

  carregarUsuariosIniciais() {
    this.isLoading = true;
    this.communityService.getAllUsers().subscribe({
      next: (dados) => {
        this.allUsers = dados || [];
        this.users = this.allUsers.slice(0, 12);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onSearchInput(event: Event) {
    const elemento = event.target as HTMLInputElement;
    this.searchSubject.next(elemento.value);
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