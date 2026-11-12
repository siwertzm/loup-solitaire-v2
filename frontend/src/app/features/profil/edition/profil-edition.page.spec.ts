import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { MoiResponse } from '../../../core/models/personnage.model';
import { AuthService } from '../../../core/services/auth.service';
import { PersonnageService } from '../../../core/services/personnage.service';
import { ProfilEditionPage } from './profil-edition.page';

// SEC-06 : le mot de passe n'est demandé (et envoyé) que si l'e-mail change.
describe('ProfilEditionPage', () => {
  const compte: MoiResponse = {
    username: 'marius',
    email: 'marius@example.com',
    emailVerifie: true,
    emailEnAttente: null,
    dateCreation: null,
    personnages: [],
  };
  let personnages: { moi: ReturnType<typeof vi.fn>; majCompte: ReturnType<typeof vi.fn> };
  let page: ProfilEditionPage;

  beforeEach(() => {
    personnages = { moi: vi.fn(() => of(compte)), majCompte: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: PersonnageService, useValue: personnages },
        { provide: AuthService, useValue: {} },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    // Logique de la page seulement, sans son gabarit.
    page = TestBed.runInInjectionContext(() => new ProfilEditionPage());
    page.ngOnInit();
  });

  it("n'envoie pas de mot de passe quand seul le nom change", () => {
    personnages.majCompte.mockReturnValue(of({ ...compte, username: 'loup' }));
    page.compteForm.patchValue({ username: 'loup' });

    expect(page.emailModifie()).toBe(false);
    page.enregistrerCompte();

    expect(personnages.majCompte).toHaveBeenCalledWith({ username: 'loup' });
    expect(page.message()).toBe('PROFIL_EDITION.SUCCES_PROFIL_MAJ');
  });

  it("exige le mot de passe quand l'e-mail change", () => {
    page.compteForm.patchValue({ email: 'nouveau@example.com' });

    expect(page.emailModifie()).toBe(true);
    page.enregistrerCompte();

    expect(personnages.majCompte).not.toHaveBeenCalled();
    expect(page.erreur()).toBe('PROFIL_EDITION.ERREUR_MDP_REQUIS_EMAIL');
  });

  it("envoie le mot de passe et affiche l'adresse en attente", () => {
    personnages.majCompte.mockReturnValue(of({ ...compte, emailEnAttente: 'nouveau@example.com' }));
    page.compteForm.patchValue({ email: 'nouveau@example.com', motDePasseActuel: 'secret' });

    page.enregistrerCompte();

    expect(personnages.majCompte).toHaveBeenCalledWith({
      username: 'marius',
      email: 'nouveau@example.com',
      motDePasseActuel: 'secret',
    });
    expect(page.emailEnAttente()).toBe('nouveau@example.com');
    // Le champ revient à l'e-mail du compte, toujours actif.
    expect(page.compteForm.controls.email.value).toBe('marius@example.com');
    expect(page.compteForm.controls.motDePasseActuel.value).toBe('');
    expect(page.message()).toBe('PROFIL_EDITION.SUCCES_EMAIL_EN_ATTENTE');
  });

  it('signale un mot de passe faux (401)', () => {
    personnages.majCompte.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    page.compteForm.patchValue({ email: 'nouveau@example.com', motDePasseActuel: 'faux' });

    page.enregistrerCompte();

    expect(page.erreur()).toBe('PROFIL_EDITION.ERREUR_MDP_ACTUEL');
  });
});
