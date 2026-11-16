import { TestBed } from '@angular/core/testing';

import { unPersonnage } from '../../../testing/fabriques';
import { InventaireSheetService } from './inventaire-sheet.service';

describe('InventaireSheetService', () => {
  let service: InventaireSheetService;

  beforeEach(() => {
    service = TestBed.inject(InventaireSheetService);
  });

  it('ouvrir puis fermer', () => {
    service.ouvrir('p1');
    expect(service.personnageId()).toBe('p1');
    expect(service.utiliserEnCombat()).toBeNull();

    service.fermer();
    expect(service.personnageId()).toBeNull();
  });

  it('REGLE-05 : le callback de combat est remis à null à la fermeture', () => {
    const utiliser = vi.fn();
    service.ouvrir('p1', utiliser);
    expect(service.utiliserEnCombat()).toBe(utiliser);

    service.fermer();
    expect(service.utiliserEnCombat()).toBeNull();
  });

  it('notifierMiseAJour publie la fiche à jour', () => {
    const personnage = unPersonnage({ enduranceActuelle: 12 });

    service.notifierMiseAJour(personnage);

    expect(service.personnageMisAJour()).toBe(personnage);
  });
});
