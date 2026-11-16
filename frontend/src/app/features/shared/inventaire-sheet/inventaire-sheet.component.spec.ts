import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { IonItemSliding } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { PersonnageResume } from '../../../core/models/personnage.model';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { ObjetService } from '../../../core/services/objet.service';
import { PersonnageService } from '../../../core/services/personnage.service';
import { InventaireSheetComponent } from './inventaire-sheet.component';

// REGLE-12 : jeter le Sac à Dos vide le Sac ; une confirmation est demandée
// avant d'appeler l'API. Les autres objets se retirent directement.
describe('InventaireSheetComponent — retrait du Sac à Dos', () => {
  let retirerObjet: ReturnType<typeof vi.fn>;
  let composant: InventaireSheetComponent;
  let glissement: IonItemSliding;

  beforeEach(() => {
    retirerObjet = vi.fn(() => of({ inventaire: [] } as unknown as PersonnageResume));
    TestBed.configureTestingModule({
      providers: [
        { provide: PersonnageService, useValue: { retirerObjet } },
        { provide: ObjetService, useValue: {} },
        {
          provide: InventaireSheetService,
          useValue: {
            personnageId: signal('p1'),
            utiliserEnCombat: signal(null),
            notifierMiseAJour: vi.fn(),
          },
        },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    // Logique seulement, sans le gabarit ni l'effet de chargement.
    composant = TestBed.runInInjectionContext(() => new InventaireSheetComponent());
    glissement = { close: vi.fn() } as unknown as IonItemSliding;
  });

  it('demande confirmation avant de jeter le Sac à Dos', () => {
    composant.demanderRetrait('sac_a_dos', glissement);

    expect(composant.retraitSacAConfirmer()).toBe(glissement);
    expect(retirerObjet).not.toHaveBeenCalled();
  });

  it('jette le Sac à Dos une fois confirmé', () => {
    composant.demanderRetrait('sac_a_dos', glissement);
    composant.confirmerRetraitSac();

    expect(retirerObjet).toHaveBeenCalledWith('p1', 'sac_a_dos', 1);
    expect(composant.retraitSacAConfirmer()).toBeNull();
  });

  it('annuler garde le Sac à Dos', () => {
    composant.demanderRetrait('sac_a_dos', glissement);
    composant.annulerRetraitSac();

    expect(retirerObjet).not.toHaveBeenCalled();
    expect(glissement.close).toHaveBeenCalled();
  });

  it('retire directement un autre Objet Spécial', () => {
    composant.demanderRetrait('carte', glissement);

    expect(retirerObjet).toHaveBeenCalledWith('p1', 'carte', 1);
  });
});

// QUAL-12 : le bonus passif des Objets Spéciaux s'affiche comme l'effet des potions.
describe('InventaireSheetComponent — bonus des Objets Spéciaux', () => {
  it('affiche +2 ENDURANCE sur le Casque, rien sur la Carte', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PersonnageService, useValue: {} },
        { provide: ObjetService, useValue: {} },
        {
          provide: InventaireSheetService,
          useValue: { personnageId: signal(null), utiliserEnCombat: signal(null) },
        },
        {
          provide: TranslateService,
          useValue: {
            instant: (cle: string, p?: { valeur: number; libelle: string }) =>
              cle === 'INVENTAIRE_SHEET.EFFET_LABEL' ? `+${p?.valeur} ${p?.libelle}` : cle,
          },
        },
      ],
    });
    const composant = TestBed.runInInjectionContext(() => new InventaireSheetComponent());
    composant.tousObjets.set([
      {
        id: 'casque',
        nom: 'Casque',
        description: null,
        categorie: 'OBJETS_SPECIAUX',
        effets: [{ type: 'ENDURANCE', valeur: 2 }],
      },
      { id: 'carte', nom: 'Carte', description: null, categorie: 'OBJETS_SPECIAUX', effets: [] },
    ]);
    composant.personnage.set({
      inventaire: [
        { objetId: 'casque', nom: 'Casque', categorie: 'OBJETS_SPECIAUX', quantite: 1 },
        { objetId: 'carte', nom: 'Carte', categorie: 'OBJETS_SPECIAUX', quantite: 1 },
      ],
    } as unknown as PersonnageResume);

    expect(composant.objetsSpeciauxDetail().map((s) => s.effetLabel)).toEqual([
      '+2 INVENTAIRE_SHEET.EFFET_ENDURANCE',
      null,
    ]);
  });
});
