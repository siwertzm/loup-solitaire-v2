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
    path: 'profil/edition',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profil/edition/profil-edition.page').then((m) => m.ProfilEditionPage),
  },
  {
    path: 'personnages/creation',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/personnages/creation/creation-personnage.page').then((m) => m.CreationPersonnagePage),
  },
  {
    path: 'personnages/:id/chapitre',
    canActivate: [authGuard],
    loadComponent: () => import('./features/game/chapitre/chapitre.page').then((m) => m.ChapitrePage),
  },
  { path: '**', redirectTo: 'accueil' },
];
