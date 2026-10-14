import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { environment } from '../../environments/environment';

export const THEMES = [
  { id: 'TEAL', label: 'Verde-água · padrão', color: '#79ddcc' },
  { id: 'BLUE', label: 'Azul', color: '#80c4ff' },
  { id: 'AMBER', label: 'Âmbar', color: '#ffd080' },
  { id: 'ROSE', label: 'Coral', color: '#ffacb5' },
  { id: 'PURPLE', label: 'Roxo clássico', color: '#bb9aff' },
  { id: 'TWILIGHT', label: 'Crepúsculo', color: 'linear-gradient(135deg,#bc9aff,#ff9bbe)' },
  { id: 'DAWN', label: 'Amanhecer', color: 'linear-gradient(135deg,#ffbf83,#ff96ac)' },
  { id: 'OCEAN', label: 'Fundo-do-mar', color: 'linear-gradient(135deg,#58dddc,#739cff)' },
  { id: 'HACKER', label: 'Hacker', color: '#87f59b' }
] as const;
export type ThemeId = typeof THEMES[number]['id'];
export function validTheme(value: unknown): ThemeId {
  return THEMES.some(t => t.id === value) ? value as ThemeId : 'TEAL';
}
export function isPublicThemeRoute(url: string): boolean {
  return /^\/(?:landing|login|register|verify|forgot-password|reset-password)(?:[/?#]|$)/.test(url) || url === '/';
}

@Injectable({providedIn:'root'})
export class ThemeService {
  private document = inject(DOCUMENT);
  private router = inject(Router);
  private http = inject(HttpClient);
  readonly selected = signal<ThemeId>('TEAL');
  private account: string | null = null;
  private generation = 0;
  constructor() {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.sync());
    this.sync();
  }
  sync() {
    const id = localStorage.getItem('token') ? localStorage.getItem('userId') : null;
    if (id !== this.account) {
      this.account = id;
      const generation = ++this.generation;
      let cached: unknown = null;
      try { cached = id ? localStorage.getItem(`mgl:theme:${id}`) : null; } catch {}
      this.selected.set(validTheme(cached));
      if (id) this.http.get<{theme?:ThemeId}>(`${environment.apiUrl}/users/${id}`).subscribe({
        next: user => { if (this.account === id && this.generation === generation) this.setSaved(user.theme); },
        error: () => {} // Cached preference remains usable offline; API remains authoritative next login.
      });
    }
    this.render();
  }
  setSaved(value: unknown) {
    ++this.generation; // An older pending fetch must not overwrite a newly saved preference.
    this.selected.set(validTheme(value));
    const id = localStorage.getItem('userId');
    try { if(id) localStorage.setItem(`mgl:theme:${id}`, this.selected()); } catch {}
    this.render();
  }
  private render() {
    const authenticated = !!localStorage.getItem('token') && !isPublicThemeRoute(this.router.url);
    this.document.documentElement.dataset['theme'] = authenticated ? this.selected().toLowerCase() : 'teal';
  }
}
