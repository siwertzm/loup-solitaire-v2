import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { TokenStorageService } from '../services/token-storage.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let tokens: { access: string; refresh: string };
  const router = { navigate: vi.fn() };
  const urlRefresh = `${environment.apiUrl}/auth/refresh`;

  beforeEach(() => {
    tokens = { access: 'ancien-access', refresh: 'ancien-refresh' };
    router.navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        {
          provide: TokenStorageService,
          useValue: {
            getAccessToken: () => tokens.access,
            getRefreshToken: () => tokens.refresh,
            setTokens: async (access: string, refresh: string) => {
              tokens = { access, refresh };
            },
            clear: vi.fn(),
          },
        },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  function deuxRequetesEn401(): { a: unknown[]; b: unknown[]; erreurs: unknown[] } {
    const a: unknown[] = [];
    const b: unknown[] = [];
    const erreurs: unknown[] = [];
    http
      .get(`${environment.apiUrl}/a`)
      .subscribe({ next: (r) => a.push(r), error: (e) => erreurs.push(e) });
    http
      .get(`${environment.apiUrl}/b`)
      .subscribe({ next: (r) => b.push(r), error: (e) => erreurs.push(e) });
    backend
      .expectOne(`${environment.apiUrl}/a`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    backend
      .expectOne(`${environment.apiUrl}/b`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    return { a, b, erreurs };
  }

  it('partage un seul refresh entre deux requetes en 401 puis les rejoue avec le nouveau jeton', () => {
    const { a, b } = deuxRequetesEn401();

    const refresh = backend.expectOne(urlRefresh);
    expect(refresh.request.body).toEqual({ refreshToken: 'ancien-refresh' });
    refresh.flush({ accessToken: 'nouvel-access', refreshToken: 'nouveau-refresh' });

    const rejouees = [
      backend.expectOne(`${environment.apiUrl}/a`),
      backend.expectOne(`${environment.apiUrl}/b`),
    ];
    for (const r of rejouees) {
      expect(r.request.headers.get('Authorization')).toBe('Bearer nouvel-access');
      r.flush('ok');
    }
    expect(a).toEqual(['ok']);
    expect(b).toEqual(['ok']);
  });

  it('vide la session et renvoie vers la connexion si le refresh echoue', () => {
    const { erreurs } = deuxRequetesEn401();

    backend.expectOne(urlRefresh).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(erreurs).toHaveLength(2);
    expect(TestBed.inject(TokenStorageService).clear).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('relance un refresh pour un 401 arrive apres la fin du precedent', () => {
    deuxRequetesEn401();
    backend.expectOne(urlRefresh).flush({ accessToken: 'n1', refreshToken: 'r1' });
    backend.match(() => true).forEach((r) => r.flush('ok'));

    http.get(`${environment.apiUrl}/c`).subscribe();
    backend
      .expectOne(`${environment.apiUrl}/c`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    const second = backend.expectOne(urlRefresh);
    expect(second.request.body).toEqual({ refreshToken: 'r1' });
    second.flush({ accessToken: 'n2', refreshToken: 'r2' });
    backend.expectOne(`${environment.apiUrl}/c`).flush('ok');
  });
});
