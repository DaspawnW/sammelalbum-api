import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { RegisterComponent } from './components/register/register';
import { MainLayoutComponent } from './components/layout/main-layout/main-layout';
import { DashboardComponent } from './components/dashboard/dashboard';
import { OfferListComponent } from './components/cards/offer-list/offer-list';
import { SearchListComponent } from './components/cards/search-list/search-list';
import { MatchListComponent } from './components/matches/match-list/match-list';
import { ExchangeListComponent } from './components/exchanges/exchange-list/exchange-list';
import { LandingPageComponent } from './components/landing-page/landing-page';
import { authGuard } from './guards/auth.guard';

import { noAuthGuard } from './guards/no-auth.guard';

export const routes: Routes = [
    { path: 'welcome', component: LandingPageComponent, canActivate: [noAuthGuard] },
    { path: 'login', component: LoginComponent, canActivate: [noAuthGuard] },
    { path: 'register', component: RegisterComponent, canActivate: [noAuthGuard] },
    { path: 'imprint', loadComponent: () => import('./components/legal/imprint/imprint').then(m => m.ImprintComponent) },
    { path: 'privacy', loadComponent: () => import('./components/legal/data-privacy/data-privacy').then(m => m.DataPrivacyComponent) },
    {
        path: '',
        component: MainLayoutComponent,
        canActivate: [authGuard],
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: DashboardComponent },
            { path: 'offers', component: OfferListComponent },
            { path: 'searches', component: SearchListComponent },
            { path: 'matches', component: MatchListComponent },
            { path: 'exchanges', component: ExchangeListComponent }
        ]
    }
];
