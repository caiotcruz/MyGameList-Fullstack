import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { CommonModule } from '@angular/common';
import { ApiResponse } from '../../models/api-response';

@Component({
  selector: 'app-verify',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './verify.html',
  styleUrl: './verify.css'
})
export class Verify implements OnInit {
  private authService = inject(AuthService);
  public router = inject(Router);
  public route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  email = '';
  codigo = '';
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit() {
    this.email = this.route.snapshot.queryParamMap.get('email') || '';
    if (!this.email) this.router.navigate(['/register']);
  }

  formatInput() {
    this.codigo = this.codigo.replace(/\D/g, '').slice(0, 6);
    this.errorMessage = '';
  }

  onVerify() {
    if (this.isLoading || this.successMessage || this.codigo.length < 6) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    
    this.cdr.detectChanges();

    this.authService.verify({ email: this.email, codigo: this.codigo }).subscribe({
      next: (res: ApiResponse<void>) => {
        this.isLoading = false; 
        this.successMessage = res.message || 'Conta verificada! Redirecionando...';
        
        this.cdr.detectChanges();
        
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.codigo = ''; 

        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Erro ao validar código.';
        }

        this.cdr.detectChanges(); 
      }
    });
  }
}