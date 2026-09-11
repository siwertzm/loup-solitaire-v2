import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideIonicAngular } from '@ionic/angular';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
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
