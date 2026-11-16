import { TestBed } from '@angular/core/testing';
import { Preferences } from '@capacitor/preferences';

import { OptionsService } from './options.service';

// Preferences tourne avec son implémentation web (localStorage) dans les tests.
describe('OptionsService', () => {
  const echelle = () => document.documentElement.style.getPropertyValue('--echelle-texte');

  beforeEach(async () => {
    await Preferences.clear();
    document.documentElement.style.removeProperty('--echelle-texte');
  });

  it('taille normale par défaut quand rien n’est enregistré', async () => {
    const service = TestBed.inject(OptionsService);
    await service.charger();

    expect(service.tailleTexte()).toBe('NORMALE');
    expect(echelle()).toBe('1');
  });

  it('enregistre la taille choisie et l’applique tout de suite', async () => {
    const service = TestBed.inject(OptionsService);

    await service.definirTailleTexte('GRANDE');

    expect(echelle()).toBe('1.15');
    expect(document.documentElement.dataset['tailleTexte']).toBe('GRANDE');
    expect((await Preferences.get({ key: 'loup_taille_texte' })).value).toBe('GRANDE');
  });

  it('relit la taille enregistrée au démarrage', async () => {
    await Preferences.set({ key: 'loup_taille_texte', value: 'PETITE' });
    const service = TestBed.inject(OptionsService);

    await service.charger();

    expect(service.tailleTexte()).toBe('PETITE');
    expect(echelle()).toBe('0.9');
  });

  it('ignore une valeur enregistrée inconnue', async () => {
    await Preferences.set({ key: 'loup_taille_texte', value: 'ENORME' });
    const service = TestBed.inject(OptionsService);

    await service.charger();

    expect(service.tailleTexte()).toBe('NORMALE');
  });
});
