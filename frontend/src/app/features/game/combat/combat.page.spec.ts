import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { CombatResponse } from '../../../core/models/combat.model';
import { PersonnageResume } from '../../../core/models/personnage.model';
import { CombatService } from '../../../core/services/combat.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../core/services/personnage.service';
import { CombatPage } from './combat.page';

// REGLE-11 : au 260 (combat à mains nues), l'HABILETÉ affichée est
// base - 4, comme celle calculée par le serveur, quelles que soient les armes.
describe('CombatPage — combat à mains nues', () => {
  let page: CombatPage;

  function personnage(habilite: number): PersonnageResume {
    return {
      id: 'p1',
      nom: 'Loup',
      habiliteBase: 15,
      habilite,
      habiliteTemp: 0,
      enduranceMax: 25,
      enduranceActuelle: 25,
      disciplines: [],
      armeMaitrisee: 'epee',
      chapitreActuelId: 260,
      volEnAttente: null,
      mort: false,
      inventaire: [],
    };
  }

  function combat(malusMainsNues: number | null): CombatResponse {
    return {
      id: 'c1',
      chapitreId: 260,
      ennemis: [],
      assautsLivres: 0,
      endurancePerdue: false,
      bonusHabiliteEnAttente: 0,
      statut: 'EN_COURS',
      fuitePossible: false,
      malusMainsNues,
      dernierTour: null,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: CombatService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: InventaireSheetService, useValue: { personnageMisAJour: signal(null) } },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    // Logique de la page seulement, sans son gabarit.
    page = TestBed.runInInjectionContext(() => new CombatPage());
  });

  it("avec l'arme maîtrisée, affiche 15 - 4 = 11 (et non 15 + 2 - 4)", () => {
    page.personnage.set(personnage(17));
    page.combat.set(combat(-4));

    expect(page.habiliteJoueur()).toBe(11);
    expect(page.detailHabiliteAffiche()).toBe('15-4');
  });

  it("remplace le picto de l'arme maîtrisée par celui des mains nues", () => {
    page.personnage.set({
      ...personnage(17),
      armeMaitrisee: 'Épée',
      inventaire: [{ objetId: 'epee', nom: 'Épée', categorie: 'ARME', quantite: 1 }],
    });

    page.combat.set(combat(null));
    expect(page.bonusArmeMaitrisee()).toBe('Épée');
    expect(page.malusMainsNues()).toBeNull();

    page.combat.set(combat(-4));
    expect(page.bonusArmeMaitrisee()).toBeNull();
    expect(page.malusMainsNues()).toBe(-4);
  });

  it('sans arme, affiche 11 (pas de -4 en plus)', () => {
    page.personnage.set(personnage(11));
    page.combat.set(combat(-4));

    expect(page.habiliteJoueur()).toBe(11);
  });

  it("dans un combat ordinaire, garde l'ajustement d'arme", () => {
    page.personnage.set(personnage(17));
    page.combat.set(combat(null));

    expect(page.habiliteJoueur()).toBe(17);
    expect(page.detailHabiliteAffiche()).toBe('15+2');
  });
});
