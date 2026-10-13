import { ChangeDetectorRef, Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router'; 
import { AuthService } from '../../services/auth';
import { NotificationService } from '../../services/notification';
import { MessageService } from '../../services/message';
import { filter, takeUntil } from 'rxjs/operators';
import { Subject, Subscription, forkJoin } from 'rxjs';
import { errorMessage } from '../../services/feedback';

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
  messageService = inject(MessageService);

  private cdr = inject(ChangeDetectorRef);
  private destroyed$ = new Subject<void>();
  private notificationRequest?: Subscription;
  notificationsLoading = false;
  notificationsError = '';
  unreadCount = 0;
  unreadMessages = 0;
  notifications: any[] = [];
  showDropdown = false;

  private intervalId: any;

  ngOnInit() {
    this.checarNotificacoes();
    this.checarMensagens();

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd), takeUntil(this.destroyed$)
    ).subscribe(() => {
      this.checarNotificacoes();
      this.checarMensagens();
    });

    this.intervalId = setInterval(() => {
      this.checarNotificacoes();
      this.checarMensagens();
    }, 30000);
  }

  ngOnDestroy() {
    this.destroyed$.next();
    this.destroyed$.complete();
    this.notificationRequest?.unsubscribe();
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  checarNotificacoes() {
    if (this.shouldShowNavbar()) {
      this.notificationService.getUnreadCount().pipe(takeUntil(this.destroyed$)).subscribe({
        next: (count) => { this.unreadCount = count; this.cdr.markForCheck(); },
        error: () => { this.unreadCount = 0; this.cdr.markForCheck(); }
      });
    }
  }

  checarMensagens() {
    if (this.shouldShowNavbar()) {
      this.messageService.getUnreadCount().pipe(takeUntil(this.destroyed$)).subscribe({
        next: (count) => { this.unreadMessages = count; this.cdr.markForCheck(); },
        error: () => { this.unreadMessages = 0; this.cdr.markForCheck(); }
      });
    }
  }

  get myId(): string | null {
    return localStorage.getItem('userId');
  }

  sair() {
    this.resetNotifications();
    this.authService.logout();
    this.router.navigate(['/landing']);
  }

  shouldShowNavbar(): boolean {
    if (!this.authService.isLoggedIn()) return false;
    const currentUrl = this.router.url;
    return !(currentUrl.includes('/login') || currentUrl.includes('/register'));
  }

  toggleNotifications() {
    this.showDropdown = !this.showDropdown;
    this.notificationRequest?.unsubscribe();
    if (!this.showDropdown) {
      this.notificationsLoading = false;
      return;
    }
    this.loadNotifications();
  }

  loadNotifications() {
    this.notificationRequest?.unsubscribe();
    this.notificationsLoading = true;
    this.notificationsError = '';
    this.notificationRequest = this.notificationService.getNotifications()
      .pipe(takeUntil(this.destroyed$)).subscribe({
        next: data => {
          this.notifications = data;
          this.notificationsLoading = false;
          this.cdr.markForCheck();
          const unread = data.filter(n => !n.read);
          if (unread.length) {
            // Mark only the displayed notifications, not newer unseen arrivals.
            forkJoin(unread.map(n => this.notificationService.markAsRead(n.id)))
              .pipe(takeUntil(this.destroyed$)).subscribe({
                next: () => {
                  unread.forEach(n => n.read = true);
                  this.checarNotificacoes();
                  this.cdr.markForCheck();
                },
                error: () => {
                  this.notificationsError = 'Não foi possível marcar como lidas. Tente novamente.';
                  this.cdr.markForCheck();
                }
              });
          }
        },
        error: err => {
          this.notificationsLoading = false;
          this.notificationsError = errorMessage(err);
          this.cdr.markForCheck();
        }
      });
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
    this.notificationRequest?.unsubscribe();
    this.notificationsLoading = false;
    this.notificationsError = ''; 
    this.notifications = [];
    this.unreadCount = 0;
    this.showDropdown = false;
  }
}