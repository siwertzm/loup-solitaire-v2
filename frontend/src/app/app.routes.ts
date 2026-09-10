import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'accueil' },
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./features/auth/register/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'accueil',
    canActivate: [authGuard],
    loadComponent: () => import('./features/accueil/accueil.page').then((m) => m.AccueilPage),
  },
  {
    path: 'profil',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profil/profil.page').then((m) => m.ProfilPage),
  },
  {
    path: 'personnages',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/personnages/liste/personnages-liste.page').then((m) => m.PersonnagesListePage),
  },
  {
    path: 'personnages/:id/chapitre',
    canActivate: [authGuard],
    loadComponent: () => import('./features/game/chapitre/chapitre.page').then((m) => m.ChapitrePage),
  },
  { path: '**', redirectTo: 'accueil' },
];
