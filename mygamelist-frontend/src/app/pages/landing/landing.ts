import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth'; // Seu serviço de Auth

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class Landing implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);

  ngOnInit() {
    // Se já estiver logado, redireciona para a Home (Feed)
    if (this.authService.isLoggedIn()) {
       this.router.navigate(['/home']);
    }
  }
}