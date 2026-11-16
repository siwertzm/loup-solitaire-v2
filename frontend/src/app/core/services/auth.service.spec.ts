import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

describe('AuthService', () => {
  const base = `${environment.apiUrl}/auth`;
  const jetons = { accessToken: 'a2', refreshToken: 'r2', tokenType: 'Bearer' };
  let service: AuthService;
  let backend: HttpTestingController;
  let stockage: {
    setTokens: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
    getRefreshToken: () => string;
  };

  beforeEach(() => {
    sessionStorage.clear();
    stockage = { setTokens: vi.fn(), clear: vi.fn(), getRefreshToken: () => 'r1' };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TokenStorageService, useValue: stockage },
      ],
    });
    service = TestBed.inject(AuthService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('login enregistre les jetons', () => {
    service.login({ identifiant: 'loup', password: 'secret' }).subscribe();

    const req = backend.expectOne(`${base}/login`);
    expect(req.request.method).toBe('POST');
    req.flush(jetons);
    expect(stockage.setTokens).toHaveBeenCalledWith('a2', 'r2');
  });

  it('changePassword enregistre les nouveaux jetons', () => {
    service.changePassword('ancien', 'nouveau').subscribe();

    const req = backend.expectOne(`${base}/me/password`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ currentPassword: 'ancien', newPassword: 'nouveau' });
    req.flush(jetons);
    expect(stockage.setTokens).toHaveBeenCalledWith('a2', 'r2');
  });

  it('logout envoie le refresh token et vide la session', () => {
    service.logout().subscribe();

    const req = backend.expectOne(`${base}/logout`);
    expect(req.request.body).toEqual({ refreshToken: 'r1' });
    req.flush(null);
    expect(stockage.clear).toHaveBeenCalled();
    expect(service.estConnecte()).toBe(false);
  });

  it('supprimerCompte envoie le mot de passe et vide la session', () => {
    service.supprimerCompte('secret').subscribe();

    const req = backend.expectOne(`${base}/me`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toEqual({ password: 'secret' });
    req.flush(null);
    expect(stockage.clear).toHaveBeenCalled();
  });

  describe('mot de passe oublié', () => {
    it("une nouvelle demande garde l'e-mail et efface l'ancien resetToken", () => {
      sessionStorage.setItem('loup_password_reset_token', 'ancien');

      service.forgotPassword('loup@exemple.fr').subscribe();
      backend.expectOne(`${base}/forgot-password`).flush(null);

      expect(service.getPasswordResetEmail()).toBe('loup@exemple.fr');
      expect(service.getPasswordResetToken()).toBeNull();
    });

    it('verifyResetCode garde le resetToken reçu', () => {
      service.verifyResetCode('loup@exemple.fr', '123456').subscribe();
      const req = backend.expectOne(`${base}/verify-reset-code`);
      expect(req.request.body).toEqual({ email: 'loup@exemple.fr', code: '123456' });
      req.flush({ resetToken: 'jeton-reset' });

      expect(service.getPasswordResetToken()).toBe('jeton-reset');
    });

    it('resetPassword nettoie tout le parcours', () => {
      sessionStorage.setItem('loup_password_reset_email', 'loup@exemple.fr');
      sessionStorage.setItem('loup_password_reset_token', 'jeton-reset');

      service.resetPassword('jeton-reset', 'nouveau').subscribe();
      backend.expectOne(`${base}/reset-password`).flush(null);

      expect(service.getPasswordResetEmail()).toBeNull();
      expect(service.getPasswordResetToken()).toBeNull();
    });
  });
});
