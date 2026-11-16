import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import {
  InventaireItem,
  ObjetResume,
  PersonnageResume,
} from '../../../../core/models/personnage.model';
import { ChapitreService } from '../../../../core/services/chapitre.service';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../../core/services/personnage.service';
import { ChapitreObjetsComponent } from './chapitre-objets.component';

// REGLE-12 : sans Sac à Dos, les objets et repas du chapitre ne peuvent pas
// être pris (badge SANS SAC) ; les armes, l'or et les Objets Spéciaux si.
describe('ChapitreObjetsComponent — Sac à Dos', () => {
  const catalogue: ObjetResume[] = [
    { id: 'repas', nom: 'Repas', description: null, categorie: 'REPAS', effets: [] },
    { id: 'potion_de_soin', nom: 'Potion', description: null, categorie: 'OBJET', effets: [] },
    { id: 'epee', nom: 'Épée', description: null, categorie: 'ARME', effets: [] },
    {
      id: 'sac_a_dos',
      nom: 'Sac à Dos',
      description: null,
      categorie: 'OBJETS_SPECIAUX',
      effets: [],
    },
  ];
  const sac: InventaireItem = {
    objetId: 'sac_a_dos',
    nom: 'Sac à Dos',
    categorie: 'OBJETS_SPECIAUX',
    quantite: 1,
  };

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
      chapitreActuelId: 20,
      volEnAttente: null,
      mort: false,
      inventaire,
    };
  }

  function creer(inventaire: InventaireItem[]): ChapitreObjetsComponent {
    TestBed.configureTestingModule({
      providers: [
        { provide: ChapitreService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: InventaireSheetService, useValue: { personnageMisAJour: signal(null) } },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    // Logique seulement, sans le gabarit.
    TestBed.overrideComponent(ChapitreObjetsComponent, { set: { template: '', imports: [] } });
    const fixture = TestBed.createComponent(ChapitreObjetsComponent);
    fixture.componentRef.setInput('objets', []);
    fixture.componentRef.setInput('tousObjets', catalogue);
    fixture.componentRef.setInput('personnage', personnage(inventaire));
    return fixture.componentInstance;
  }

  it('bloque objets et repas sans Sac à Dos', () => {
    const composant = creer([]);

    expect(composant.sansSac('repas')).toBe(true);
    expect(composant.sansSac('potion_de_soin')).toBe(true);
  });

  it('laisse prendre une arme ou le Sac lui-même sans Sac à Dos', () => {
    const composant = creer([]);

    expect(composant.sansSac('epee')).toBe(false);
    expect(composant.sansSac('sac_a_dos')).toBe(false);
  });

  it('ne bloque rien avec le Sac à Dos', () => {
    const composant = creer([sac]);

    expect(composant.sansSac('repas')).toBe(false);
  });
});
