import { ApplicationConfig, ErrorHandler, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideIonicAngular } from '@ionic/angular';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import * as Sentry from '@sentry/angular';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { OptionsService } from './core/services/options.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // OPS-04 : les erreurs non gérées passent par Sentry (qui les affiche
    // aussi dans la console). Sans DSN, Sentry n'envoie rien.
    { provide: ErrorHandler, useValue: Sentry.createErrorHandler() },
    provideZonelessChangeDetection(),
    // QUAL-15 : taille du texte appliquée avant le premier affichage.
    provideAppInitializer(() => inject(OptionsService).charger()),
    provideIonicAngular({}),
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: '/assets/i18n/', suffix: '.json' }),
      fallbackLang: 'fr',
      lang: 'fr',
    }),
  ],
};