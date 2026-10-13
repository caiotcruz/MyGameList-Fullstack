import { authGuard } from './guards/auth.guard';
import { Routes } from '@angular/router';


export const routes: Routes = [
    { path: 'privacy', canActivate: [authGuard], loadComponent: () => import('./pages/privacy/privacy').then(m => m.Privacy) },
    { path: '', redirectTo: 'landing', pathMatch: 'full' },
    { path: 'login', loadComponent: () => import('./pages/login/login').then(m => m.Login) },
    { path: 'search', canActivate: [authGuard], loadComponent: () => import('./pages/search/search').then(m => m.Search) },
    { path: 'community', canActivate: [authGuard], loadComponent: () => import('./pages/community/community').then(m => m.Community) },
    { path: 'profile/:id', canActivate: [authGuard], loadComponent: () => import('./pages/profile/profile').then(m => m.Profile) },
    { path: 'register', loadComponent: () => import('./pages/register/register').then(m => m.Register)},
    { path: 'home', canActivate: [authGuard], loadComponent: () => import('./pages/home/home').then(m => m.Home) },
    { path: 'game/:id', canActivate: [authGuard], loadComponent: () => import('./pages/game-details/game-details').then(m => m.GameDetails) },
    { path: 'messages', canActivate: [authGuard], loadComponent: () => import('./pages/messages/messages').then(m => m.Messages) },
    { path: 'messages/:userId', canActivate: [authGuard], loadComponent: () => import('./pages/messages/messages').then(m => m.Messages) },
    { path: 'landing', loadComponent: () => import('./pages/landing/landing').then(m => m.Landing) },
    { path: 'verify', loadComponent: () => import('./pages/verify/verify').then(m => m.Verify) },
    { path: 'forgot-password', loadComponent: () => import('./pages/forgot-password/forgot-password').then(m => m.ForgotPassword) },
    { path: 'reset-password', loadComponent: () => import('./pages/reset-password/reset-password').then(m => m.ResetPassword) },
    { path: '**', redirectTo: 'landing' }
];