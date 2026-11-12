import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { signal } from '@angular/core';

import { ChapitreResponse, LienResponse } from '../../../core/models/chapitre.model';
import {
  InventaireItem,
  ObjetResume,
  PersonnageResume,
} from '../../../core/models/personnage.model';
import { ChapitreService } from '../../../core/services/chapitre.service';
import { CombatService } from '../../../core/services/combat.service';
import { DisciplineService } from '../../../core/services/discipline.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { ObjetService } from '../../../core/services/objet.service';
import { PersonnageService } from '../../../core/services/personnage.service';
import { ChapitrePage } from './chapitre.page';

// REGLE-16 : avant de quitter un chapitre, l'alerte signale aussi un échange
// encore possible (ex. Marteau du 307), en plus des objets non ramassés.
describe('ChapitrePage — échange non fait', () => {
  const marteau: ObjetResume = {
    id: 'marteau',
    nom: 'Marteau de Guerre',
    description: null,
    categorie: 'ARME',
    effets: [],
  };
  const hache: InventaireItem = { objetId: 'hache', nom: 'Hache', categorie: 'ARME', quantite: 1 };
  const lien: LienResponse = { chapitreCibleId: 165, disponible: true, conditions: [] };

  let avancer: ReturnType<typeof vi.fn>;
  let page: ChapitrePage;

  function chapitre307(objets: ChapitreResponse['objets'] = []): ChapitreResponse {
    return {
      id: 307,
      text: '',
      combat: false,
      tirageHasard: null,
      ennemis: [],
      effets: [
        {
          type: 'ECHANGE',
          valeur: 1,
          nom: 'echange',
          conditions: [{ type: 'ARME', targetId: 'marteau', valeur: '1' }],
        },
      ],
      liens: [lien],
      objets,
    };
  }

  function personnage(inventaire: InventaireItem[]): PersonnageResume {
    return {
      id: 'p1',
      nom: 'Loup',
      habiliteBase: 15,
      habilite: 15,
      habiliteTemp: 0,
      enduranceMax: 25,
      enduranceActuelle: 25,
      disciplines: [],
      armeMaitrisee: null,
      chapitreActuelId: 307,
      volEnAttente: null,
      mort: false,
      inventaire,
    };
  }

  beforeEach(() => {
    avancer = vi.fn(() => ({ subscribe: vi.fn() }));
    TestBed.configureTestingModule({
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ChapitreService, useValue: { avancerVersChapitre: avancer } },
        { provide: CombatService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: DisciplineService, useValue: {} },
        { provide: ObjetService, useValue: {} },
        { provide: InventaireSheetService, useValue: { personnageMisAJour: signal(null) } },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    // Logique de la page seulement, sans son gabarit.
    page = TestBed.runInInjectionContext(() => new ChapitrePage());
    page.personnageId.set('p1');
    page.chargement.set(false);
    page.tousObjets.set([marteau]);
    page.chapitre.set(chapitre307());
  });

  it("signale l'échange quand le joueur a une arme à donner", () => {
    page.personnage.set(personnage([hache]));

    expect(page.echangesNonFaits()).toEqual([marteau]);
    page.choisirLien(lien);

    expect(page.lienEnAttenteConfirmation()).toBe(lien);
    expect(avancer).not.toHaveBeenCalled();
  });

  it("ne signale rien si l'échange est déjà fait", () => {
    page.personnage.set(personnage([{ ...hache, objetId: 'marteau', nom: 'Marteau de Guerre' }]));

    expect(page.echangesNonFaits()).toEqual([]);
    page.choisirLien(lien);

    expect(page.lienEnAttenteConfirmation()).toBeNull();
    expect(avancer).toHaveBeenCalledWith('p1', 165);
  });

  it("ne signale rien si le joueur n'a aucune arme à donner", () => {
    page.personnage.set(
      personnage([{ objetId: 'repas', nom: 'Repas', categorie: 'REPAS', quantite: 2 }]),
    );

    expect(page.echangesNonFaits()).toEqual([]);
  });

  it("signale à la fois les objets non pris et l'échange", () => {
    page.personnage.set(personnage([hache]));
    page.chapitre.set(
      chapitre307([{ objetId: 'repas', nom: 'Repas', valeur: 1, optionnel: true }]),
    );
    page.restantsObjets.set({ repas: 1 });

    expect(page.objetsNonPris().length).toBe(1);
    expect(page.echangesNonFaits().length).toBe(1);
    page.choisirLien(lien);

    expect(page.lienEnAttenteConfirmation()).toBe(lien);
  });

  it("« voir l'échange » ferme le popup et ouvre l'onglet Effets", () => {
    page.personnage.set(personnage([hache]));
    page.choisirLien(lien);

    page.voirEchange();

    expect(page.lienEnAttenteConfirmation()).toBeNull();
    expect(page.ongletActif()).toBe('effets');
  });
});
