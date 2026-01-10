import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms'; // <--- Para usar ngModel
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule], // <--- Importante!
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  authService = inject(AuthService);
  router = inject(Router);

  // Variáveis que estarão ligadas ao HTML
  email = '';
  password = '';

  onLogin() {
    const credentials = { email: this.email, password: this.password };

    this.authService.login(credentials).subscribe({
      next: (token) => {
        console.log('Login Sucesso! Token:', token);
        // Aqui vamos redirecionar para a Home depois. Por enquanto só loga.
        this.router.navigate(['/search']);
      },
      error: (err) => {
        console.error('Erro:', err);
        alert('Email ou senha inválidos');
      }
    });
  }
}