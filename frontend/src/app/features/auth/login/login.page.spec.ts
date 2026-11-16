import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  let login: ReturnType<typeof vi.fn>;
  let renvoyer: ReturnType<typeof vi.fn>;
  let navigateByUrl: ReturnType<typeof vi.fn>;
  let page: LoginPage;

  const echec = (status: number) =>
    throwError(
      () => new HttpErrorResponse({ status, error: { message: 'texte brut du serveur' } }),
    );

  beforeEach(() => {
    login = vi.fn(() => of({}));
    renvoyer = vi.fn(() => of(undefined));
    navigateByUrl = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { login, resendVerification: renvoyer } },
        { provide: Router, useValue: { navigateByUrl } },
      ],
    });
    page = TestBed.runInInjectionContext(() => new LoginPage());
    page.form.setValue({ identifiant: ' loup ', password: 'secret' });
  });

  it("formulaire incomplet : pas d'appel", () => {
    page.form.setValue({ identifiant: '', password: '' });

    page.soumettre();

    expect(login).not.toHaveBeenCalled();
  });

  it("succès : vers l'accueil", () => {
    page.soumettre();

    expect(navigateByUrl).toHaveBeenCalledWith('/accueil');
  });

  it('compte non vérifié (403) : propose de renvoyer le lien, avec l’identifiant nettoyé', () => {
    login.mockReturnValue(echec(403));

    page.soumettre();
    page.renvoyerVerification();

    expect(page.compteNonVerifie()).toBe(true);
    expect(page.erreur()).toBeNull();
    expect(renvoyer).toHaveBeenCalledWith('loup');
    expect(page.messageRenvoi()).toBe('AUTH.LOGIN.LIEN_RENVOYE');
  });

  it('erreur du serveur : clé traduite, jamais le message brut (QUAL-08)', () => {
    login.mockReturnValue(echec(401));

    page.soumettre();

    expect(page.erreur()).toBe('AUTH.LOGIN.ERREUR_IDENTIFIANTS');
  });

  it('trop de tentatives (429) : message dédié', () => {
    login.mockReturnValue(echec(429));

    page.soumettre();

    expect(page.erreur()).toBe('AUTH.ERREUR_TROP_DE_TENTATIVES');
  });
});
