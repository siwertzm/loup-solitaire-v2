import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { TokenStorageService } from '../services/token-storage.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  const urlLogin = {} as UrlTree;
  let refreshToken: string | null;
  let pret: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    pret = vi.fn(() => Promise.resolve());
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { createUrlTree: vi.fn(() => urlLogin) } },
        {
          provide: TokenStorageService,
          useValue: { whenReady: pret, getRefreshToken: () => refreshToken },
        },
      ],
    });
  });

  const lancer = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  it('laisse passer avec un refresh token, après whenReady()', async () => {
    refreshToken = 'jeton';

    expect(await lancer()).toBe(true);
    expect(pret).toHaveBeenCalled();
  });

  it('renvoie vers /auth/login sans refresh token', async () => {
    refreshToken = null;

    expect(await lancer()).toBe(urlLogin);
    expect(TestBed.inject(Router).createUrlTree).toHaveBeenCalledWith(['/auth/login']);
  });
});
