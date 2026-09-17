import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';

// Routes publiques côté backend : /auth/** (sauf /auth/me) et /actuator/health.
// Voir backend/README.md > Authentification.
const ROUTES_PUBLIQUES = ['/auth/register', '/auth/login', '/auth/refresh', '/auth/verify-email', '/auth/forgot-password', '/auth/reset-password'];

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
      //
      // IMPORTANT : le catchError ci-dessous est placé AVANT le switchMap et ne
      // capture donc QUE l'échec de authService.refresh() lui-même (refresh
      // token invalide/expiré/révoqué -> déconnexion forcée justifiée). Si on le
      // plaçait après (comme avant), il capturait AUSSI l'échec de la requête
      // REJOUÉE : or un 401 peut très bien être un 401 "métier" sans rapport
      // avec l'expiration du token (ex. PUT /auth/me/password avec un mauvais
      // mot de passe actuel -> BadCredentialsException -> 401). Dans ce cas le
      // refresh réussissait silencieusement, la requête rejouée échouait de
      // nouveau avec 401 pour la même raison (mauvais mot de passe), et
      // l'ancien code interprétait à tort ce second échec comme une session
      // invalide -> déconnexion + redirection vers /auth/login. Désormais, une
      // erreur de la requête rejouée est simplement retransmise à l'appelant
      // (ex. ProfilEditionPage), sans provoquer de déconnexion.
      return authService.refresh().pipe(
        catchError((erreurRefresh: unknown) => {
          authService.clearSessionLocale();
          router.navigate(['/auth/login']);
          return throwError(() => erreurRefresh);
        }),
        switchMap((res) => {
          const rejouee = req.clone({ setHeaders: { Authorization: `Bearer ${res.accessToken}` } });
          return next(rejouee);
        }),
      );
    }),
  );
};