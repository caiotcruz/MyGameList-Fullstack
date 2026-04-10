import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPassword implements OnInit {
  private route = inject(ActivatedRoute);
  router = inject(Router);
  private authService = inject(AuthService);

  email = '';
  codigo = '';
  newPassword = '';
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    const email = this.route.snapshot.queryParamMap.get('email');

    if (!token || !email) {
      this.router.navigate(['/login']);
      return;
    }

    this.codigo = token;
    this.email = email;
  }

  onReset() {
    if (!this.codigo || this.newPassword.length < 6) {
      this.errorMessage = 'Dados inválidos ou senha muito curta.';
      return;
    }
    
    this.isLoading = true;
    this.errorMessage = '';

    const data = { 
      email: this.email, 
      codigo: this.codigo, 
      newPassword: this.newPassword 
    };

    this.authService.resetPassword(data).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.successMessage = 'Senha alterada com sucesso!';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Token inválido ou expirado.';
      }
    });
  }
}