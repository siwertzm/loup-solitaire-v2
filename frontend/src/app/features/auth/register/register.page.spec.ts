import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { RegisterPage } from './register.page';

// SEC-05 : l'inscription répond pareil qu'un e-mail ait déjà un compte ou
// non ; seul un nom d'utilisateur pris (409) est signalé.
describe('RegisterPage', () => {
  let register: ReturnType<typeof vi.fn>;
  let page: RegisterPage;

  beforeEach(() => {
    register = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { register } },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    // Logique de la page seulement, sans son gabarit.
    page = TestBed.runInInjectionContext(() => new RegisterPage());
    page.form.setValue({
      username: 'marius',
      email: 'marius@example.com',
      password: 'motdepasse123',
      confirmation: 'motdepasse123',
    });
  });

  it("affiche l'écran de succès sur 202, sans envoyer la confirmation", () => {
    register.mockReturnValue(of(undefined));

    page.soumettre();

    expect(register).toHaveBeenCalledWith({
      username: 'marius',
      email: 'marius@example.com',
      password: 'motdepasse123',
    });
    expect(page.succes()).toBe(true);
  });

  it("signale un nom d'utilisateur pris (409)", () => {
    register.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    page.soumettre();

    expect(page.erreur()).toBe('AUTH.REGISTER.ERREUR_NOM_PRIS');
    expect(page.succes()).toBe(false);
  });

  it('distingue la limite de débit (429) et les autres erreurs', () => {
    register.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 429 })));
    page.soumettre();
    expect(page.erreur()).toBe('AUTH.ERREUR_TROP_DE_TENTATIVES');

    register.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    page.soumettre();
    expect(page.erreur()).toBe('AUTH.REGISTER.ERREUR_INSCRIPTION');
  });
});
