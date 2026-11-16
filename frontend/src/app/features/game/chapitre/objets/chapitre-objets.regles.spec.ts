import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { InventaireItem } from '../../../../core/models/personnage.model';
import { ChapitreService } from '../../../../core/services/chapitre.service';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../../core/services/personnage.service';
import { unItem, unObjet, unPersonnage } from '../../../../../testing/fabriques';
import { ChapitreObjetsComponent } from './chapitre-objets.component';

// QUAL-10 : plafonds d'inventaire de CLAUDE.md recopiés dans le front pour
// l'affichage (2 armes, 8 objets + repas cumulés, 50 Pièces d'Or, objets
// spéciaux illimités). Un test casse si une valeur change.
describe('ChapitreObjetsComponent — plafonds et quantités', () => {
  const catalogue = [
    unObjet('epee', 'ARME', { nom: 'Épée' }),
    unObjet('hache', 'ARME', { nom: 'Hache' }),
    unObjet('repas', 'REPAS'),
    unObjet('potion', 'OBJET'),
    unObjet('or', 'BOURSE'),
    unObjet('carte', 'OBJETS_SPECIAUX'),
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ChapitreService, useValue: {} },
        { provide: PersonnageService, useValue: {} },
        { provide: InventaireSheetService, useValue: { personnageMisAJour: signal(null) } },
        { provide: TranslateService, useValue: { instant: (cle: string) => cle } },
      ],
    });
    TestBed.overrideComponent(ChapitreObjetsComponent, { set: { template: '', imports: [] } });
  });

  function creer(
    inventaire: InventaireItem[],
    armeMaitrisee: string | null = null,
  ): ChapitreObjetsComponent {
    const fixture = TestBed.createComponent(ChapitreObjetsComponent);
    fixture.componentRef.setInput('objets', []);
    fixture.componentRef.setInput('tousObjets', catalogue);
    fixture.componentRef.setInput('personnage', unPersonnage({ inventaire, armeMaitrisee }));
    fixture.componentRef.setInput('restants', { epee: 1, repas: 0 });
    return fixture.componentInstance;
  }

  it('2 armes au plus', () => {
    expect(creer([unItem('epee', 'ARME')]).estPlein('ARME')).toBe(false);
    expect(creer([unItem('epee', 'ARME'), unItem('hache', 'ARME')]).estPlein('ARME')).toBe(true);
  });

  it('8 objets et repas cumulés au plus', () => {
    expect(
      creer([unItem('repas', 'REPAS', 4), unItem('potion', 'OBJET', 3)]).estPlein('REPAS'),
    ).toBe(false);
    const plein = creer([unItem('repas', 'REPAS', 5), unItem('potion', 'OBJET', 3)]);
    expect(plein.estPlein('REPAS')).toBe(true);
    expect(plein.estPlein('OBJET')).toBe(true);
  });

  it("50 Pièces d'Or au plus", () => {
    expect(creer([unItem('or', 'BOURSE', 49)]).estPlein('BOURSE')).toBe(false);
    expect(creer([unItem('or', 'BOURSE', 50)]).estPlein('BOURSE')).toBe(true);
  });

  it('objets spéciaux jamais pleins', () => {
    // Bonus de la Maîtrise des Armes affiché dans le popup « libérer de la place ».
    expect(creer([]).bonusArmeMaitrisee).toBe(2);
    expect(creer([unItem('carte', 'OBJETS_SPECIAUX', 999)]).estPlein('OBJETS_SPECIAUX')).toBe(
      false,
    );
  });

  it('restant : quantité du parent, 0 par défaut', () => {
    const composant = creer([]);

    expect(composant.restant('epee')).toBe(1);
    expect(composant.restant('repas')).toBe(0);
    expect(composant.restant('inconnu')).toBe(0);
  });

  it('categorieObjet insensible à la casse, null si inconnu', () => {
    const composant = creer([]);

    expect(composant.categorieObjet('EPEE')).toBe('ARME');
    expect(composant.categorieObjet('inconnu')).toBeNull();
  });

  it("itemsDeCategorie marque l'arme maîtrisée", () => {
    const composant = creer(
      [unItem('epee', 'ARME', 1, 'Épée'), unItem('hache', 'ARME', 1, 'Hache')],
      'Épée',
    );

    expect(composant.itemsDeCategorie('ARME').map((i) => [i.objetId, i.maitrisee])).toEqual([
      ['epee', true],
      ['hache', false],
    ]);
  });
});
