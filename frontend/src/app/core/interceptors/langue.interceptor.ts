import { HttpInterceptorFn } from '@angular/common/http';

import { environment } from '../../../environments/environment';

/**
 * DATA-19 : langue des textes du livre demandée à l'API (en-tête X-Langue,
 * lu par util/Textes côté serveur). Elle vient de <html lang>, que
 * OptionsService tient à jour : pas d'injection, donc pas de boucle avec le
 * chargeur de traductions, qui passe lui aussi par HttpClient.
 */
export const langueInterceptor: HttpInterceptorFn = (req, next) => {
  const langue = document.documentElement.lang;
  if (!langue || !req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'X-Langue': langue } }));
};
