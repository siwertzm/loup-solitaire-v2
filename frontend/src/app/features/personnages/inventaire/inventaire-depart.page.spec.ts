import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { InventaireItem } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { InventaireDepartPage } from './inventaire-depart.page';

// L'objet de départ tiré est « le seul objet hors équipement fixe » : depuis
// REGLE-12, le Sac à Dos fait partie de l'équipement fixe et ne doit jamais
// être présenté comme l'objet tiré, quel que soit l'ordre de l'inventaire.
describe('InventaireDepartPage — objet de départ', () => {
  const fixe: InventaireItem[] = [
    { objetId: 'sac_a_dos', nom: 'Sac à Dos', categorie: 'OBJETS_SPECIAUX', quantite: 1 },
    { objetId: 'hache', nom: 'Hache', categorie: 'ARME', quantite: 1 },
    { objetId: 'repas', nom: 'Repas', categorie: 'REPAS', quantite: 1 },
    { objetId: 'carte', nom: 'Carte', categorie: 'OBJETS_SPECIAUX', quantite: 1 },
    { objetId: 'coin', nom: 'Pièce Premium', categorie: 'OBJETS_SPECIAUX', quantite: 999 },
  ];
  const casque: InventaireItem = {
    objetId: 'casque',
    nom: 'Casque',
    categorie: 'OBJETS_SPECIAUX',
    quantite: 1,
  };

  function creer(inventaire: InventaireItem[]): InventaireDepartPage {
    TestBed.configureTestingModule({
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: PersonnageService, useValue: {} },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new InventaireDepartPage());
    page['inventaire'].set(inventaire);
    return page;
  }

  it("ne présente jamais le Sac à Dos comme l'objet tiré", () => {
    expect(creer([...fixe, casque]).nomObjet()).toBe('Casque');
  });

  it("trouve l'objet tiré même placé avant le Sac à Dos", () => {
    expect(creer([casque, ...fixe]).nomObjet()).toBe('Casque');
  });
});
