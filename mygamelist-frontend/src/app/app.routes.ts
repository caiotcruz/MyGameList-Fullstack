import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Search } from './pages/search/search';
import { MyList} from './pages/my-list/my-list'
import { Community } from './pages/community/community';
import { Profile } from './pages/profile/profile';

export const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Login },
    { path: 'search', component: Search },
    { path: 'my-list', component: MyList },
    { path: 'community', component: Community },
    { path: 'profile/:id', component: Profile }
];