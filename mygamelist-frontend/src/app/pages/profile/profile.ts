import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { CommunityService } from '../../services/community';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {
  route = inject(ActivatedRoute);
  communityService = inject(CommunityService);
  cdr = inject(ChangeDetectorRef);

  userId: number = 0;
  userGames: any[] = [];
  userName: string = 'Usuário';

  ngOnInit() {
    this.userId = Number(this.route.snapshot.paramMap.get('id'));

    if (this.userId) {
      this.carregarPerfil();
    }
  }

  carregarPerfil() {
    this.communityService.getUserList(this.userId).subscribe({
      next: (dados: any) => {
        this.userGames = dados;
        
        if (this.userGames.length > 0) {
          this.userName = this.userGames[0].user.name;
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar perfil', err);
        this.userName = 'Usuário não encontrado ou lista vazia';
        this.cdr.detectChanges();
      }
    });
  }

  getStatusColor(status: string): string {
    switch(status) {
      case 'PLAYING': return '#4caf50';
      case 'COMPLETED': return '#2196f3';
      case 'DROPPED': return '#f44336';
      case 'PLAN_TO_PLAY': return '#9e9e9e';
      default: return '#000';
    }
  }
}