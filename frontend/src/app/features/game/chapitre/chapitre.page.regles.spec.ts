import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { LienResponse } from '../../../core/models/chapitre.model';
import { ChapitreService } from '../../../core/services/chapitre.service';
import { CombatService } from '../../../core/services/combat.service';
import { DisciplineService } from '../../../core/services/discipline.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { ObjetService } from '../../../core/services/objet.service';
import { PersonnageService } from '../../../core/services/personnage.service';
import { unChapitre, unObjetChap, unPersonnage } from '../../../../testing/fabriques';
import { ChapitrePage } from './chapitre.page';

// QUAL-10 : logique d'affichage de la page chapitre.
describe('ChapitrePage — règles affichées', () => {
  const kraan = { id: 'kraan', nom: 'Kraan', habilite: 16, endurance: 24 };
  const vordak = { id: 'vordak', nom: 'Vordak', habilite: 17, endurance: 25 };
  const lien = (
    chapitreCibleId: number,
    conditions: LienResponse['conditions'] = [],
    disponible = true,
  ) => ({
    chapitreCibleId,
    disponible,
    conditions,
  });
  let page: ChapitrePage;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ChapitreService, useValue: {} },
        { provide: CombatService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: DisciplineService, useValue: {} },
        { provide: ObjetService, useValue: {} },
        { provide: InventaireSheetService, useValue: { personnageMisAJour: signal(null) } },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    page = TestBed.runInInjectionContext(() => new ChapitrePage());
    page.personnage.set(unPersonnage());
  });

  it('objetsNonPris : seuls les gains avec un restant > 0', () => {
    page.chapitre.set(
      unChapitre({
        objets: [unObjetChap('epee', 1), unObjetChap('repas', 2), unObjetChap('or', -10, false)],
      }),
    );
    page.restantsObjets.set({ epee: 1, repas: 0, or: 10 });

    expect(page.objetsNonPris().map((o) => o.objetId)).toEqual(['epee']);
  });

  describe('segmentsTexte', () => {
    it('un ennemi par jeton [[ENNEMI]], dans l’ordre', () => {
      page.chapitre.set(
        unChapitre({ text: 'Avant [[ENNEMI]] milieu [[ENNEMI]] après', ennemis: [kraan, vordak] }),
      );

      expect(page.segmentsTexte()).toEqual([
        { type: 'texte', html: 'Avant' },
        { type: 'ennemi', ennemi: kraan },
        { type: 'texte', html: 'milieu' },
        { type: 'ennemi', ennemi: vordak },
        { type: 'texte', html: 'après' },
      ]);
    });

    it('texte vide ignoré, jeton sans ennemi ignoré', () => {
      page.chapitre.set(unChapitre({ text: '[[ENNEMI]]  [[ENNEMI]]', ennemis: [kraan] }));

      expect(page.segmentsTexte()).toEqual([{ type: 'ennemi', ennemi: kraan }]);
    });
  });

  it('liensEvitementCombat : lien EVITEMENT disponible, pas la fuite à 0 assaut', () => {
    const evitement = lien(248, [{ type: 'EVITEMENT', targetId: null, valeur: null }]);
    const fuite0 = lien(7, [{ type: 'FUITE', targetId: null, valeur: '0' }]);
    const evitementIndispo = lien(22, [{ type: 'EVITEMENT', targetId: null, valeur: null }], false);
    page.chapitre.set(unChapitre({ liens: [evitement, fuite0, evitementIndispo, lien(1)] }));

    expect(page.liensEvitementCombat()).toEqual([evitement]);
  });

  it.each([
    ['VICTOIRE', true],
    ['FUITE', false],
  ])('combatGagne : combat %s → %s (DATA-18, libellé du lien EVITEMENT)', (statut, attendu) => {
    Object.assign(TestBed.inject(CombatService), { recuperer: () => of({ statut }) });

    page['actualiserCombatDisponible'](true, 'p1');

    expect(page.combatGagne()).toBe(attendu);
  });

  it('trouverLienRetourNarratif : le lien conditionné au coin', () => {
    const retour = lien(1, [{ type: 'OBJET', targetId: 'coin', valeur: '1' }]);
    page.chapitre.set(unChapitre({ liens: [lien(2), retour] }));

    expect(page['trouverLienRetourNarratif']()).toEqual(retour);
  });

  describe('2102 : mort et ENDURANCE masquées jusqu’au dernier dé', () => {
    beforeEach(() => {
      page.personnage.set(unPersonnage({ mort: true, enduranceActuelle: 0 }));
      page.enduranceAvant2102.set(9);
    });

    it('avant le dernier dé : pas encore mort, ancienne ENDURANCE', () => {
      page.chapitre.set(unChapitre({ id: 2102 }));

      expect(page.mortAffichee()).toBe(false);
      expect(page.enduranceAffichee()).toBe(9);
    });

    it('après le dé : état réel', () => {
      page.chapitre.set(unChapitre({ id: 2102 }));
      page.hasardTermine.set(true);

      expect(page.mortAffichee()).toBe(true);
      expect(page.enduranceAffichee()).toBe(0);
    });

    it('autre chapitre : état réel', () => {
      page.chapitre.set(unChapitre({ id: 21 }));

      expect(page.mortAffichee()).toBe(true);
    });
  });
});
