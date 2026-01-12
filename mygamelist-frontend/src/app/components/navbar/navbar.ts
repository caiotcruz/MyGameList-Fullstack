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
export class Navbar implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);

  myId: string | null = null;

  ngOnInit() {
    this.myId = localStorage.getItem('userId');
  }

  sair() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}