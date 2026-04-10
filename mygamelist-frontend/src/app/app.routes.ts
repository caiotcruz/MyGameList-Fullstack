import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Search } from './pages/search/search';
import { Community } from './pages/community/community';
import { Profile } from './pages/profile/profile';
import { Register } from './pages/register/register';
import { Home } from './pages/home/home';
import { GameDetails } from './pages/game-details/game-details';
import { Landing } from './pages/landing/landing';
import { Verify } from './pages/verify/verify';
import { ForgotPassword } from './pages/forgot-password/forgot-password';
import { ResetPassword } from './pages/reset-password/reset-password';


export const routes: Routes = [
    { path: '', redirectTo: 'landing', pathMatch: 'full' },
    { path: 'login', component: Login },
    { path: 'search', component: Search },
    { path: 'community', component: Community },
    { path: 'profile/:id', component: Profile },
    { path: 'register', component: Register},
    { path: 'home', component: Home },
    { path: 'game/:id', component: GameDetails },
    { path: 'landing', component: Landing },
    { path: 'verify', component: Verify },
    { path: 'forgot-password', component: ForgotPassword },
    { path: 'reset-password', component: ResetPassword }
];