import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { EffetResponse } from '../../../../core/models/chapitre.model';
import { InventaireItem, PersonnageResume } from '../../../../core/models/personnage.model';
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
