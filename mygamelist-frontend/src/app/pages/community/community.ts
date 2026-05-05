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
  suggestions: any[] = [];
  allUsers: any[] = []; 
  searchQuery = '';
  isLoading = false;

  isModalOpen = false;
  selectedMutuals: any[] = [];
  selectedUserName = '';

  private searchSubject = new Subject<string>();

  ngOnInit() {
    this.carregarSugestoes(); 
    this.carregarUsuariosIniciais();
    
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(), 
      switchMap(termo => {
        this.isLoading = true;
        this.cdr.detectChanges();
        if (!termo.trim()) return of(this.allUsers.slice(0, 12));
        
        return this.communityService.getUserListByName(termo).pipe(
          catchError(() => of([])) 
        );
      })
    ).subscribe((res: any) => {
      this.users = res.data || res; 
      this.isLoading = false;
      this.cdr.detectChanges();
    });
  }

  carregarSugestoes() {
    this.communityService.getSuggestions().subscribe({
      next: (res: any) => {
        this.suggestions = res.data || [];
        this.cdr.detectChanges();
      }
    });
  }

  carregarUsuariosIniciais() {
    this.isLoading = true;
    this.communityService.getAllUsers().subscribe({
      next: (res: any) => {
        const listaVindaDoBack = res.data || res;

        this.allUsers = listaVindaDoBack.filter((u: any) => 
          !u.isFollowing && 
          !this.suggestions.some(s => s.id === u.id)
        );

        this.users = this.allUsers.slice(0, 12);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirModalMutual(user: any, event: Event) {
    event.stopPropagation();
    this.selectedUserName = user.name;
    this.selectedMutuals = user.mutualFriends;
    this.isModalOpen = true;
    this.cdr.detectChanges();
  }

  onSearchInput(event: Event) {
    const elemento = event.target as HTMLInputElement;
    this.searchSubject.next(elemento.value);
  }

  verPerfil(userId: number) {
    this.router.navigate(['/profile', userId]);
  }

  verPerfilDesdeModal(friendId: number) {
    this.isModalOpen = false;
    this.verPerfil(friendId);
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