import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { EffetResponse } from '../../../../core/models/chapitre.model';
import {
  InventaireItem,
  ObjetResume,
  PersonnageResume,
} from '../../../../core/models/personnage.model';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../../core/services/personnage.service';
import { ChapitreEffetsComponent } from './chapitre-effets.component';

// REGLE-09 : au 144, le voleur prend un objet ou un repas du Sac ; une arme
// seulement si le Sac n'en contient aucun.
describe('ChapitreEffetsComponent — vol du 144', () => {
  const vol144: EffetResponse = { type: 'VOL', valeur: 1, nom: 'vol', conditions: [] };
  const hache: InventaireItem = { objetId: 'hache', nom: 'Hache', categorie: 'ARME', quantite: 1 };
  const repas: InventaireItem = { objetId: 'repas', nom: 'Repas', categorie: 'REPAS', quantite: 2 };
  const carte: InventaireItem = {
    objetId: 'carte',
    nom: 'Carte',
    categorie: 'OBJETS_SPECIAUX',
    quantite: 1,
  };

  function creer(inventaire: InventaireItem[]): ChapitreEffetsComponent {
    TestBed.configureTestingModule({
      providers: [
        { provide: InventaireSheetService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    // Logique seulement, sans le gabarit.
    TestBed.overrideComponent(ChapitreEffetsComponent, { set: { template: '', imports: [] } });
    const fixture = TestBed.createComponent(ChapitreEffetsComponent);
    fixture.componentRef.setInput('effets', [vol144]);
    fixture.componentRef.setInput('personnage', { inventaire } as PersonnageResume);
    return fixture.componentInstance;
  }

  it('ne propose que le contenu du Sac quand il y en a', () => {
    expect(creer([hache, repas, carte]).itemsEligiblesVol(vol144)).toEqual([repas]);
  });

  it('propose les armes quand le Sac est vide', () => {
    expect(creer([hache, carte]).itemsEligiblesVol(vol144)).toEqual([hache]);
  });
});

// QUAL-16 : l'arme proposée par un échange (Marteau de l'ermite, 307) est
// signalée si c'est l'arme de la Maîtrise des Armes.
describe("ChapitreEffetsComponent — échange d'une arme maîtrisée", () => {
  const echange: EffetResponse = {
    type: 'ECHANGE',
    valeur: 1,
    nom: 'echange',
    conditions: [{ type: 'ARME', targetId: 'marteau', valeur: '1' }],
  };
  const catalogue: ObjetResume[] = [
    { id: 'marteau', nom: 'Marteau de Guerre', description: null, categorie: 'ARME', effets: [] },
  ];

  function creer(armeMaitrisee: string | null): ChapitreEffetsComponent {
    TestBed.configureTestingModule({
      providers: [
        { provide: InventaireSheetService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    TestBed.overrideComponent(ChapitreEffetsComponent, { set: { template: '', imports: [] } });
    const fixture = TestBed.createComponent(ChapitreEffetsComponent);
    fixture.componentRef.setInput('effets', [echange]);
    fixture.componentRef.setInput('tousObjets', catalogue);
    fixture.componentRef.setInput('personnage', {
      armeMaitrisee,
      inventaire: [],
    } as unknown as PersonnageResume);
    return fixture.componentInstance;
  }

  it("signale l'arme proposée quand elle est maîtrisée", () => {
    expect(creer('Marteau de Guerre').armeProposeeMaitrisee('marteau')).toBe(true);
  });

  it('ne signale rien pour une autre Maîtrise', () => {
    expect(creer('Épée').armeProposeeMaitrisee('marteau')).toBe(false);
  });
});
