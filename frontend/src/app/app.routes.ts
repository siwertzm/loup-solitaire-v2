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
    path: 'regle/intro',
    canActivate: [authGuard],
    loadComponent: () => import('./features/regle/intro/regle-intro.page').then((m) => m.RegleIntroPage),
  },
  {
    path: 'regle/disciplines',
    canActivate: [authGuard],
    loadComponent: () => import('./features/regle/discipline/regle-discipline.page').then((m) => m.RegleDisciplinePage),
  },
  {
    path: 'regle/equipement',
    canActivate: [authGuard],
    loadComponent: () => import('./features/regle/equipement/regle-equipement.page').then((m) => m.RegleEquipementPage),
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
    path: 'personnages/intro',
    canActivate: [authGuard],
    loadComponent: () => import('./features/personnages/creation/intro/intro-creation.page').then((m) => m.IntroCreationPage),
  },
  {
    path: 'personnages/:id/inventaire/intro',
    canActivate: [authGuard],
    loadComponent: () => import('./features/personnages/inventaire/intro/intro-inventaire.page').then((m) => m.IntroInventairePage),
  },
  {
    path: 'personnages/:id/inventaire/depart',
    canActivate: [authGuard],
    loadComponent: () => import('./features/personnages/inventaire/inventaire-depart.page').then((m) => m.InventaireDepartPage),
  },
  {
    path: 'personnages/:id/chapitre',
    canActivate: [authGuard],
    loadComponent: () => import('./features/game/chapitre/chapitre.page').then((m) => m.ChapitrePage),
  },
  { path: '**', redirectTo: 'accueil' },
];
