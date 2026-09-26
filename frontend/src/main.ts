import { bootstrapApplication } from '@angular/platform-browser';
import * as Sentry from '@sentry/angular';

import { appConfig } from './app/app.config';
import { App } from './app/app';
import { environment } from './environments/environment';

// OPS-04 : plantages de l'app envoyés à Sentry (seulement si un DSN est
// configuré). RGPD : aucune donnée personnelle (IP, cookies, en-têtes).
if (environment.sentryDsn) {
  Sentry.init({
    dsn: environment.sentryDsn,
    environment: environment.sentryEnvironment,
    // Pas d'identité, de cookies, d'en-têtes, de corps HTTP ni de
    // paramètres d'URL (qui peuvent contenir des jetons).
    dataCollection: {
      userInfo: false,
      cookies: false,
      httpHeaders: false,
      httpBodies: [],
      urlQueryParams: false,
    },
  });
}

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));