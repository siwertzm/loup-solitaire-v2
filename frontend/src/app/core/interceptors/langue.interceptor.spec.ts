import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { langueInterceptor } from './langue.interceptor';

describe('langueInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([langueInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    document.documentElement.lang = '';
  });

  it('envoie la langue courante à l’API', () => {
    document.documentElement.lang = 'en';

    http.get(`${environment.apiUrl}/chapitres/1`).subscribe();

    expect(
      backend.expectOne(`${environment.apiUrl}/chapitres/1`).request.headers.get('X-Langue'),
    ).toBe('en');
  });

  it('n’ajoute rien hors de l’API (fichiers de traduction)', () => {
    document.documentElement.lang = 'en';

    http.get('/assets/i18n/en.json').subscribe();

    expect(backend.expectOne('/assets/i18n/en.json').request.headers.has('X-Langue')).toBe(false);
  });

  it('n’ajoute rien tant que la langue n’est pas connue', () => {
    http.get(`${environment.apiUrl}/objets`).subscribe();

    expect(backend.expectOne(`${environment.apiUrl}/objets`).request.headers.has('X-Langue')).toBe(
      false,
    );
  });
});
