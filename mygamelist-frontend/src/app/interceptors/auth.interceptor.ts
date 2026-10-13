import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

import { isApiRequest } from './api-url';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  if (!isApiRequest(req.url, environment.apiUrl, window.location.origin)) return next(req);
  const pathname = new URL(req.url, window.location.origin).pathname;
  const isPublicAuth = /\/auth\/(login|register|verify|forgot-password|reset-password)$/.test(pathname);
  const token = localStorage.getItem('token');
  const request = token && !isPublicAuth ? req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  }) : req;

  return next(request).pipe(catchError(error => {
    if (error.status === 401 && !isPublicAuth) {
      for (const key of ['token', 'userId', 'userName']) localStorage.removeItem(key);
      router.navigate(['/login']);
    }
    return throwError(() => error);
  }));
};
