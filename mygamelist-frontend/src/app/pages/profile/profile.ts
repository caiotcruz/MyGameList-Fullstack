import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router'; // <--- Para ler a URL
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
  userName: string = 'Usuário'; // Placeholder até carregar

  ngOnInit() {
    // 1. Pega o ID da URL (ex: /profile/5 -> id = 5)
    this.userId = Number(this.route.snapshot.paramMap.get('id'));

    if (this.userId) {
      this.carregarPerfil();
    }
  }

  carregarPerfil() {
    this.communityService.getUserList(this.userId).subscribe({
      next: (dados: any) => {
        this.userGames = dados;
        
        // Tenta descobrir o nome do usuário pelo primeiro item da lista (se tiver)
        if (this.userGames.length > 0) {
          this.userName = this.userGames[0].user.name;
        }

        this.cdr.detectChanges(); // Força atualização
      },
      error: (err) => {
        console.error('Erro ao carregar perfil', err);
        this.userName = 'Usuário não encontrado ou lista vazia';
        this.cdr.detectChanges();
      }
    });
  }

  // Helper de cores (igual ao da sua lista)
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