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
  
  private intervalId: any;

  ngOnInit() {
    this.checarNotificacoes();

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.checarNotificacoes();
    });

    this.intervalId = setInterval(() => {
      this.checarNotificacoes();
    }, 30000); 
  }

  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  checarNotificacoes() {
    if (this.shouldShowNavbar()) {
      this.notificationService.getUnreadCount().subscribe({
        next: (count) => this.unreadCount = count,
        error: () => this.unreadCount = 0
      });
    }
  }


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

      const unread = this.notifications.filter(n => !n.read);

      if (unread.length > 0) {
        unread.forEach(n => n.read = true);
        this.unreadCount = 0;

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