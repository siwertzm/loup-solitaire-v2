import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { CombatService } from '../../../core/services/combat.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../core/services/personnage.service';
import { unCombat, unEnnemiDeCombat, unPersonnage } from '../../../../testing/fabriques';
import { CombatPage } from './combat.page';

// QUAL-10 : HABILETÉ et jauges affichées pendant le combat. L'ajustement
// d'arme (+2 maîtrisée, -4 sans arme) vient du serveur (habilite -
// habiliteBase) ; le +2 de Puissance Psychique est recopié dans le front.
describe('CombatPage — règles affichées', () => {
  let page: CombatPage;

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
    page = TestBed.runInInjectionContext(() => new CombatPage());
    page.combat.set(unCombat());
  });

  describe('HABILETÉ affichée', () => {
    it('arme maîtrisée : +2 (ajustement du serveur)', () => {
      page.personnage.set(unPersonnage({ habiliteBase: 15, habilite: 17 }));

      expect(page.habiliteJoueur()).toBe(17);
      expect(page.detailHabiliteAffiche()).toBe('15+2');
    });

    it('sans arme : -4', () => {
      page.personnage.set(unPersonnage({ habiliteBase: 15, habilite: 11 }));

      expect(page.habiliteJoueur()).toBe(11);
      expect(page.detailHabiliteAffiche()).toBe('15-4');
    });

    it('Puissance Psychique : +2', () => {
      page.personnage.set(unPersonnage({ disciplines: ['PUISSANCE_PSYCHIQUE'] }));

      expect(page.bonusPsychiqueAffiche()).toBe(2);
      expect(page.habiliteJoueur()).toBe(17);
    });

    it('Puissance Psychique : rien contre un ennemi qui y résiste', () => {
      page.personnage.set(unPersonnage({ disciplines: ['PUISSANCE_PSYCHIQUE'] }));
      page.combat.set(
        unCombat({ ennemis: [unEnnemiDeCombat({ resistances: ['PUISSANCE_PSYCHIQUE'] })] }),
      );

      expect(page.bonusPsychiqueAffiche()).toBe(0);
    });

    it('bonus temporaire et bonus de garde s’ajoutent', () => {
      page.personnage.set(unPersonnage({ habiliteTemp: 1 }));
      page.combat.set(unCombat({ bonusHabiliteEnAttente: 3 }));

      expect(page.habiliteJoueur()).toBe(19);
      expect(page.detailHabiliteAffiche()).toBe('15+1+3');
    });

    it('aucun ajustement : pas de détail', () => {
      page.personnage.set(unPersonnage());

      expect(page.detailHabiliteAffiche()).toBeNull();
    });
  });

  describe('ennemiActif', () => {
    const vaincu = unEnnemiDeCombat({ id: 'a', actif: false, vaincu: true });
    const actif = unEnnemiDeCombat({ id: 'b', actif: true });
    const suivant = unEnnemiDeCombat({ id: 'c', actif: false });

    it("l'ennemi actif, sinon le premier non vaincu, sinon un vaincu", () => {
      page.combat.set(unCombat({ ennemis: [vaincu, actif, suivant] }));
      expect(page.ennemiActif()?.id).toBe('b');

      page.combat.set(unCombat({ ennemis: [vaincu, { ...suivant }] }));
      expect(page.ennemiActif()?.id).toBe('c');

      page.combat.set(unCombat({ ennemis: [vaincu] }));
      expect(page.ennemiActif()?.id).toBe('a');
    });
  });

  describe('jauges', () => {
    it('pourcentages et couleurs (> 50 % haut, > 20 % moyen, sinon bas)', () => {
      page.personnage.set(unPersonnage({ enduranceMax: 20, enduranceActuelle: 10 }));
      page.combat.set(
        unCombat({ ennemis: [unEnnemiDeCombat({ enduranceMax: 20, enduranceActuelle: 3 })] }),
      );

      expect(page.pctJoueur()).toBe(50);
      expect(page.classeBarreJoueur()).toBe('moyen');
      expect(page.pctEnnemi()).toBe(15);
      expect(page.classeBarreEnnemi()).toBe('bas');
    });

    it('à zéro : 0 %, barre basse', () => {
      page.personnage.set(unPersonnage({ enduranceMax: 20, enduranceActuelle: 0 }));

      expect(page.pctJoueur()).toBe(0);
      expect(page.classeBarreJoueur()).toBe('bas');
    });
  });

  it('round et fuite viennent du serveur', () => {
    page.combat.set(unCombat({ assautsLivres: 2, fuitePossible: true }));

    expect(page.round()).toBe(3);
    expect(page.fuitePossible()).toBe(true);
  });
});
