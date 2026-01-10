import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  authService = inject(AuthService);
  router = inject(Router);

  email = '';
  password = '';

  onLogin() {
    const credentials = { email: this.email, password: this.password };

    this.authService.login(credentials).subscribe({
      next: (token) => {
        console.log('Login Sucesso! Token:', token);
        this.router.navigate(['/search']);
      },
      error: (err) => {
        console.error('Erro:', err);
        alert('Email ou senha inválidos');
      }
    });
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}