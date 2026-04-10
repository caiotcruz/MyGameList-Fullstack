import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPassword {
  public router = inject(Router);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef); 

  email = '';
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  emailSent = false;

  onSubmit() {
    if (!this.email || this.isLoading) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges(); 
    this.authService.forgotPassword(this.email).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.emailSent = true;
        this.successMessage = res.message;
        
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erro ao recuperar:', err);
        
        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Erro ao processar solicitação.';
        }
        
        this.cdr.detectChanges();
      }
    });
  }
}