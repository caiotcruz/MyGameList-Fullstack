import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router'; 
import { AuthService } from '../../services/auth';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';
import { ApiResponse } from '../../models/api-response';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink, CommonModule], 
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  private authService = inject(AuthService);
  public router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  name = '';
  email = '';
  password = '';

  isLoading = false;
  errorMessage = '';

  onRegister() {
    if (this.isLoading) return;

    if (!this.name || !this.email || !this.password) {
      this.errorMessage = 'Preencha todos os campos.';
      return;
    }

    if (this.password.length < 6) {
      this.errorMessage = 'A senha deve ter pelo menos 6 caracteres.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges(); 

    const userData = { 
      name: this.name, 
      email: this.email, 
      password: this.password 
    };

    this.authService.register(userData)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges(); 
        })
      )
      .subscribe({
        next: (res: ApiResponse<void>) => {
          console.log(res.message);
          this.router.navigate(['/verify'], { 
            queryParams: { email: this.email } 
          });
        },
        error: (err) => {
          console.error('Erro no registro:', err);

          if (err.error && err.error.message) {
            this.errorMessage = err.error.message;
          } else if (err.status === 0) {
            this.errorMessage = 'Servidor offline. Tente mais tarde.';
          } else {
            this.errorMessage = 'Erro inesperado ao criar conta.';
          }
        }
      });
  }
}