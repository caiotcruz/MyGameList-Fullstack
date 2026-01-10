import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive], // Importe RouterLink aqui!
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {
  authService = inject(AuthService);
  router = inject(Router);

  sair() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}