import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommunityService } from '../../services/community';
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
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);
  
  users: any[] = [];

  ngOnInit() {
    this.communityService.getAllUsers().subscribe({
      next: (dados) => {
        this.users = dados,
        this.cdr.detectChanges();
      },
      error: (err) => { 
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  verPerfil(userId: number) {
    // Vamos criar essa rota no próximo passo
    this.router.navigate(['/profile', userId]);
  }
}