import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';

// Routes publiques côté backend : /auth/** (sauf /auth/me) et /actuator/health.
// Voir backend/README.md > Authentification.
const ROUTES_PUBLIQUES = ['/auth/register', '/auth/login', '/auth/refresh', '/auth/verify-email'];

function estRoutePublique(url: string): boolean {
  return ROUTES_PUBLIQUES.some((route) => url.includes(route));
}

function estRequeteApi(url: string): boolean {
  return url.startsWith(environment.apiUrl);
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);
  const router = inject(Router);

  const cibleApi = estRequeteApi(req.url);
  const accessToken = tokenStorage.getAccessToken();

  const reqAvecToken =
    cibleApi && accessToken && !estRoutePublique(req.url)
      ? req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
      : req;

  return next(reqAvecToken).pipe(
    catchError((erreur: unknown) => {
      const est401 =
        erreur instanceof HttpErrorResponse && erreur.status === 401 && cibleApi && !estRoutePublique(req.url);

      if (!est401) {
        return throwError(() => erreur);
      }

      // Token d'accès expiré (15 min par défaut) : on tente un refresh silencieux
      // puis on rejoue la requête initiale avec le nouveau token.
      return authService.refresh().pipe(
        switchMap((res) => {
          const rejouee = req.clone({ setHeaders: { Authorization: `Bearer ${res.accessToken}` } });
          return next(rejouee);
        }),
        catchError((erreurRefresh: unknown) => {
          // Refresh token lui-même invalide/expiré/révoqué : déconnexion forcée.
          authService.clearSessionLocale();
          router.navigate(['/auth/login']);
          return throwError(() => erreurRefresh);
        }),
      );
    }),
  );
};
