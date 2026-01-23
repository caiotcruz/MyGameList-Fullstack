import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router'; 
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink], 
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  authService = inject(AuthService);
  router = inject(Router);

  name = '';
  email = '';
  password = '';

  onRegister() {
    const userData = { 
      name: this.name, 
      email: this.email, 
      password: this.password 
    };

    this.authService.register(userData).subscribe({
      next: () => {
        alert('Conta criada com sucesso!');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Erro no registro:', err);
        alert('Erro ao criar conta. Verifique se o email já existe.');
      }
    });
  }
}