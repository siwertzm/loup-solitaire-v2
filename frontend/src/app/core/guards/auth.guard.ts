import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { TokenStorageService } from '../services/token-storage.service';

/**
 * Vérifie la présence d'un refresh token local avant d'autoriser l'accès.
 * La validité réelle est de toute façon vérifiée serveur-side à chaque appel API ;
 * ce guard évite juste d'afficher un écran de jeu à un visiteur non connecté.
 */
export const authGuard: CanActivateFn = async () => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  await tokenStorage.whenReady();

  if (tokenStorage.getRefreshToken()) {
    return true;
  }

  return router.createUrlTree(['/auth/login']);
};
