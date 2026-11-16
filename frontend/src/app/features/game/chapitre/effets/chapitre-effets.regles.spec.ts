import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { CondResponse, EffetResponse } from '../../../../core/models/chapitre.model';
import { InventaireItem } from '../../../../core/models/personnage.model';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../../core/services/personnage.service';
import { unEffet, unItem, unObjet, unPersonnage } from '../../../../../testing/fabriques';
import { ChapitreEffetsComponent } from './chapitre-effets.component';

// QUAL-10 : logique d'affichage des effets (protections, repas, vol, échange).
describe('ChapitreEffetsComponent — règles affichées', () => {
  const cond = (type: string, targetId: string | null, valeur = '1'): CondResponse => ({
    type,
    targetId,
    valeur,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: InventaireSheetService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    TestBed.overrideComponent(ChapitreEffetsComponent, { set: { template: '', imports: [] } });
  });

  function creer(
    effets: EffetResponse[],
    inventaire: InventaireItem[] = [],
    disciplines: string[] = [],
    volEnAttente: string | null = null,
  ): ChapitreEffetsComponent {
    const fixture = TestBed.createComponent(ChapitreEffetsComponent);
    fixture.componentRef.setInput('effets', effets);
    fixture.componentRef.setInput('tousObjets', [
      unObjet('marteau', 'ARME'),
      unObjet('torche', 'OBJET'),
    ]);
    fixture.componentRef.setInput(
      'personnage',
      unPersonnage({ inventaire, disciplines, volEnAttente }),
    );
    return fixture.componentInstance;
  }

  describe('effetEvite : toutes les protections doivent être possédées', () => {
    // Ex. : il faut la Torche ET le Briquet pour éviter la perte.
    const torche = cond('OBJET', 'torche');
    const briquet = cond('OBJET', 'briquet');
    const effet = unEffet('ENDURANCE', { valeur: -2, conditions: [torche, briquet] });

    it('les deux objets : effet évité', () => {
      const c = creer([effet], [unItem('torche', 'OBJET'), unItem('briquet', 'OBJET')]);
      expect(c.effetEvite(torche)).toBe(true);
    });

    it('un seul des deux : effet subi', () => {
      expect(creer([effet], [unItem('torche', 'OBJET')]).effetEvite(torche)).toBe(false);
    });

    it('protection par discipline', () => {
      const chasse = cond('DISCIPLINE', 'chasse');
      const repas = unEffet('ENDURANCE', { valeur: -3, conditions: [chasse] });

      expect(creer([repas], [], ['CHASSE']).effetEvite(chasse)).toBe(true);
      expect(creer([repas], [], ['CAMOUFLAGE']).effetEvite(chasse)).toBe(false);
    });
  });

  describe('statutRepas', () => {
    it('plafonds et bonus recopiés de CLAUDE.md', () => {
      const c = creer([]);
      expect([c.maxArmes, c.maxObjetsEtRepas, c.maxBourse, c.bonusArmeMaitrisee]).toEqual([
        2, 8, 50, 2,
      ]);
    });

    const repas = unEffet('REPAS', { valeur: -1 });

    it('Chasse : dispensé', () => {
      expect(creer([repas], [], ['CHASSE']).statutRepas(repas)).toBe('CHASSE');
    });

    it('avec un Repas : consommé', () => {
      expect(creer([repas], [unItem('repas', 'REPAS')]).statutRepas(repas)).toBe('REPAS_CONSOMME');
    });

    it('sans Repas : -3 ENDURANCE', () => {
      expect(creer([repas]).statutRepas(repas)).toBe('MALUS_ENDURANCE');
    });

    it('le résultat du serveur fait autorité', () => {
      const resolu = unEffet('REPAS', { valeur: -1, resultat: 'MALUS_ENDURANCE' });
      expect(creer([resolu], [unItem('repas', 'REPAS')]).statutRepas(resolu)).toBe(
        'MALUS_ENDURANCE',
      );
    });
  });

  describe('vol', () => {
    const volAuChoix = unEffet('VOL', { valeur: 1 });
    const volArme = unEffet('VOL', { valeur: 1, conditions: [cond('ARME', null)] });
    const perteDuSac = unEffet('VOL', { valeur: 11 });

    it('seuls les vols de valeur 1 sont interactifs', () => {
      const c = creer([]);
      expect(c.volInteractif(volAuChoix)).toBe(true);
      expect(c.volInteractif(perteDuSac)).toBe(false);
    });

    it('vol en attente selon sa portée (TOUT ou ARME)', () => {
      expect(creer([], [], [], 'TOUT').volEnAttentePourEffet(volAuChoix)).toBe(true);
      expect(creer([], [], [], 'TOUT').volEnAttentePourEffet(volArme)).toBe(false);
      expect(creer([], [], [], 'ARME').volEnAttentePourEffet(volArme)).toBe(true);
      expect(creer([]).volEnAttentePourEffet(volAuChoix)).toBe(false);
    });

    it("vol d'arme : seules les armes sont proposées", () => {
      const c = creer([], [unItem('hache', 'ARME'), unItem('repas', 'REPAS')]);
      expect(c.itemsEligiblesVol(volArme).map((i) => i.objetId)).toEqual(['hache']);
    });
  });

  describe('échange (REGLE-16)', () => {
    const marteau = cond('ARME', 'marteau');

    it('déjà fait si l’objet proposé est possédé', () => {
      expect(creer([], [unItem('marteau', 'ARME')]).echangeDejaFait(marteau)).toBe(true);
      expect(creer([], [unItem('hache', 'ARME')]).echangeDejaFait(marteau)).toBe(false);
    });

    it("on ne donne qu'un objet de la même catégorie", () => {
      const c = creer([], [unItem('hache', 'ARME'), unItem('torche', 'OBJET')]);
      expect(c.itemsEligiblesEchange(marteau).map((i) => i.objetId)).toEqual(['hache']);
    });
  });
});
