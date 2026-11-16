import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { InventaireItem, ObjetResume, PersonnageResume } from '../../core/models/personnage.model';
import { DisciplineService } from '../../core/services/discipline.service';
import { ObjetService } from '../../core/services/objet.service';
import { PersonnageService } from '../../core/services/personnage.service';
import { PersonnagePage } from './personnage.page';

// QUAL-12 : l'encart ENDURANCE détaille le bonus du Casque / de la Cotte de
// mailles sur le maximum (« 24 + 4 Max »), jamais sur la valeur affichée.
describe('PersonnagePage — bonus d’ENDURANCE des Objets Spéciaux', () => {
  const catalogue: ObjetResume[] = [
    {
      id: 'casque',
      nom: 'Casque',
      description: null,
      categorie: 'OBJETS_SPECIAUX',
      effets: [{ type: 'ENDURANCE', valeur: 2 }],
    },
    {
      id: 'cotte_de_mailles',
      nom: 'Cotte de mailles',
      description: null,
      categorie: 'OBJETS_SPECIAUX',
      effets: [{ type: 'ENDURANCE', valeur: 4 }],
    },
    { id: 'carte', nom: 'Carte', description: null, categorie: 'OBJETS_SPECIAUX', effets: [] },
  ];
  const casque: InventaireItem = {
    objetId: 'casque',
    nom: 'Casque',
    categorie: 'OBJETS_SPECIAUX',
    quantite: 1,
  };
  const cotte: InventaireItem = {
    objetId: 'cotte_de_mailles',
    nom: 'Cotte de mailles',
    categorie: 'OBJETS_SPECIAUX',
    quantite: 1,
  };
  const carte: InventaireItem = {
    objetId: 'carte',
    nom: 'Carte',
    categorie: 'OBJETS_SPECIAUX',
    quantite: 1,
  };

  function creer(inventaire: InventaireItem[]): PersonnagePage {
    TestBed.configureTestingModule({
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', 'p1']]) } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: PersonnageService, useValue: {} },
        { provide: DisciplineService, useValue: { lister: () => of([]) } },
        { provide: ObjetService, useValue: {} },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new PersonnagePage());
    page.tousObjets.set(catalogue);
    page.personnage.set({
      enduranceMax: 28,
      enduranceActuelle: 28,
      inventaire,
    } as unknown as PersonnageResume);
    return page;
  }

  it('Cotte de mailles : 24 + 4 Max', () => {
    const page = creer([cotte, carte]);

    expect(page.bonusEndurance()).toBe(4);
    expect(page.enduranceMaxBase()).toBe(24);
    expect(page.endurance()).toBe(28);
  });

  it('Casque et Cotte : un seul total, + 6', () => {
    const page = creer([casque, cotte]);

    expect(page.bonusEndurance()).toBe(6);
    expect(page.enduranceMaxBase()).toBe(22);
  });

  it('sans armure : aucun bonus', () => {
    expect(creer([carte]).bonusEndurance()).toBe(0);
  });
});
