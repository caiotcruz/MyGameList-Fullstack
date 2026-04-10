import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private authService = inject(AuthService);
  public router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  email = '';
  password = '';
  isLoading = false;
  errorMessage = '';

  onLogin() {
    if (this.isLoading) return;
    
    if (!this.email || !this.password) {
      this.errorMessage = 'Por favor, preencha todos os campos.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    const credentials = { email: this.email, password: this.password };

    this.authService.login(credentials).subscribe({
      next: (res) => { 
        this.isLoading = false;
        const userData = res.data;
        
        localStorage.setItem('token', userData.token);
        localStorage.setItem('userId', userData.userId.toString());
        localStorage.setItem('userName', userData.name);

        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erro no login:', err);

        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else if (err.status === 401 || err.status === 403) {
          this.errorMessage = 'E-mail ou senha inválidos.';
        } else {
          this.errorMessage = 'Erro ao conectar com o servidor.';
        }
        
        this.cdr.detectChanges();
      }
    });
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}