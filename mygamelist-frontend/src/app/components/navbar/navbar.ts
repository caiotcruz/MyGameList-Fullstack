import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router'; 
import { AuthService } from '../../services/auth';
import { NotificationService } from '../../services/notification';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar implements OnInit, OnDestroy {
  authService = inject(AuthService);
  router = inject(Router);
  notificationService = inject(NotificationService);

  unreadCount = 0;
  notifications: any[] = [];
  showDropdown = false;
  
  // Para limpar o intervalo quando sair
  private intervalId: any;

  ngOnInit() {
    // 1. Checa assim que carrega
    this.checarNotificacoes();

    // 2. Checa toda vez que mudar de rota (Navegar)
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.checarNotificacoes();
    });

    // 3. Checa a cada 30 segundos (Polling)
    // Isso garante que se você deixar a aba aberta, o número aparece sozinho
    this.intervalId = setInterval(() => {
      this.checarNotificacoes();
    }, 30000); 
  }

  ngOnDestroy() {
    // Limpa o timer para não pesar a memória se o componente for destruído
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  // Lógica centralizada para verificar
  checarNotificacoes() {
    if (this.shouldShowNavbar()) {
      this.notificationService.getUnreadCount().subscribe({
        next: (count) => this.unreadCount = count,
        error: () => this.unreadCount = 0
      });
    }
  }

  // --- O RESTO CONTINUA IGUAL ---

  get myId(): string | null {
    return localStorage.getItem('userId');
  }

  sair() {
    this.resetNotifications();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  shouldShowNavbar(): boolean {
    if (!this.authService.isLoggedIn()) return false;
    const currentUrl = this.router.url;
    return !(currentUrl.includes('/login') || currentUrl.includes('/register'));
  }

  toggleNotifications() {
  this.showDropdown = !this.showDropdown;

  if (this.showDropdown) {
    this.notificationService.getNotifications().subscribe(data => {
      this.notifications = data;

      // 🔥 MARCA TODAS COMO LIDAS AO ABRIR
      const unread = this.notifications.filter(n => !n.read);

      if (unread.length > 0) {
        unread.forEach(n => n.read = true);
        this.unreadCount = 0;

        // Chamada única (ideal)
        this.notificationService.markAllAsRead().subscribe();
      }
    });
  }
}

  onNotificationClick(notif: any) {
    this.showDropdown = false;

    if (notif.actor?.id) {
      this.router.navigate(['/profile', notif.actor.id]);
    }
  }

  getNotificationText(type: string): string {
    switch(type) {
      case 'LIKE': return 'curtiu sua atividade';
      case 'COMMENT': return 'comentou na sua atividade';
      case 'FOLLOW': return 'começou a te seguir';
      default: return 'interagiu com você';
    }
  }
  
  formatDate(dateStr: string): string {
      const date = new Date(dateStr);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
  }

  resetNotifications() {
    this.notifications = [];
    this.unreadCount = 0;
    this.showDropdown = false;
  }
}