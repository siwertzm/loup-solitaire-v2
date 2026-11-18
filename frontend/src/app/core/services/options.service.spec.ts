import { TestBed } from '@angular/core/testing';
import { Preferences } from '@capacitor/preferences';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { langueParDefaut, OptionsService } from './options.service';

// Preferences tourne avec son implémentation web (localStorage) dans les tests.
describe('OptionsService', () => {
  const echelle = () => document.documentElement.style.getPropertyValue('--echelle-texte');

  beforeEach(async () => {
    await Preferences.clear();
    document.documentElement.style.removeProperty('--echelle-texte');
    TestBed.configureTestingModule({
      providers: [provideTranslateService({ fallbackLang: 'fr' })],
    });
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

  describe('langue (QUAL-09)', () => {
    it('prend la langue de l’appareil si elle est proposée, sinon le français', () => {
      expect(langueParDefaut('en-GB')).toBe('en');
      expect(langueParDefaut('EN')).toBe('en');
      expect(langueParDefaut('fr-CA')).toBe('fr');
      expect(langueParDefaut('de-DE')).toBe('fr');
      expect(langueParDefaut(undefined)).toBe('fr');
    });

    it('une langue enregistrée l’emporte sur celle de l’appareil', async () => {
      await Preferences.set({ key: 'loup_langue', value: 'en' });
      vi.spyOn(navigator, 'language', 'get').mockReturnValue('fr-FR');
      const service = TestBed.inject(OptionsService);

      await service.charger();

      expect(service.langue()).toBe('en');
      expect(TestBed.inject(TranslateService).getCurrentLang()).toBe('en');
      expect(document.documentElement.lang).toBe('en');
    });

    it('sans choix enregistré, suit l’appareil', async () => {
      vi.spyOn(navigator, 'language', 'get').mockReturnValue('de-DE');
      const service = TestBed.inject(OptionsService);

      await service.charger();

      expect(service.langue()).toBe('fr');
    });

    it('enregistre la langue choisie et l’applique tout de suite', async () => {
      const service = TestBed.inject(OptionsService);

      await service.definirLangue('en');

      expect(TestBed.inject(TranslateService).getCurrentLang()).toBe('en');
      expect(document.documentElement.lang).toBe('en');
      expect((await Preferences.get({ key: 'loup_langue' })).value).toBe('en');
    });
  });
});
