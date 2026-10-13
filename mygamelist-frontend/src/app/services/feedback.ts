import { Injectable, signal } from '@angular/core';
export function errorMessage(error: any): string {
  if (error?.status === 0) return 'Não foi possível conectar. Confira sua conexão e tente novamente.';
  if (error?.status === 401) return 'Sua sessão expirou. Entre novamente; seu rascunho permanece nesta aba.';
  if (error?.status >= 500) return 'Serviço temporariamente indisponível. Tente novamente em instantes.';
  return typeof error?.error?.message === 'string' ? error.error.message : 'Não foi possível concluir a ação. Tente novamente.';
}
@Injectable({providedIn:'root'})
export class Feedback {
  readonly message = signal('');
  private timer?: ReturnType<typeof setTimeout>;
  show(message: string) { clearTimeout(this.timer); this.message.set(message); this.timer = setTimeout(() => this.message.set(''), 7000); }
  error(error: unknown) { this.show(errorMessage(error)); }
}
