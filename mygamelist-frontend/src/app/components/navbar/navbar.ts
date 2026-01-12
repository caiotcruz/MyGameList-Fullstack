import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar{
  authService = inject(AuthService);
  router = inject(Router);

  get myId(): string | null {
    return localStorage.getItem('userId');
  }

  sair() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}