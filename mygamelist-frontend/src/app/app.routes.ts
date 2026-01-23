import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Search } from './pages/search/search';
import { Community } from './pages/community/community';
import { Profile } from './pages/profile/profile';
import { Register } from './pages/register/register';
import { Home } from './pages/home/home';
import { GameDetails } from './pages/game-details/game-details';

export const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Login },
    { path: 'search', component: Search },
    { path: 'community', component: Community },
    { path: 'profile/:id', component: Profile },
    { path: 'register', component: Register},
    { path: 'home', component: Home },
    { path: 'game/:id', component: GameDetails }
];