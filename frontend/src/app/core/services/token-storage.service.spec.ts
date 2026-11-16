import { TestBed } from '@angular/core/testing';
import { Preferences } from '@capacitor/preferences';

import { TokenStorageService } from './token-storage.service';

// Preferences tourne avec son implémentation web (localStorage) dans les tests.
describe('TokenStorageService', () => {
  beforeEach(async () => {
    await Preferences.clear();
    await Preferences.set({ key: 'loup_access_token', value: 'access-stocke' });
    await Preferences.set({ key: 'loup_refresh_token', value: 'refresh-stocke' });
  });

  it("s'hydrate depuis Preferences au démarrage", async () => {
    const service = TestBed.inject(TokenStorageService);
    await service.whenReady();

    expect(service.getAccessToken()).toBe('access-stocke');
    expect(service.getRefreshToken()).toBe('refresh-stocke');
  });

  it("enregistrerJetons met à jour la copie synchrone avant la fin de l'écriture", async () => {
    const service = TestBed.inject(TokenStorageService);
    await service.whenReady();

    const ecriture = service.enregistrerJetons('nouvel-access', 'nouveau-refresh');

    expect(service.getAccessToken()).toBe('nouvel-access');
    expect(service.getRefreshToken()).toBe('nouveau-refresh');
    await ecriture;
    expect((await Preferences.get({ key: 'loup_refresh_token' })).value).toBe('nouveau-refresh');
  });

  it('clear vide les deux jetons', async () => {
    const service = TestBed.inject(TokenStorageService);
    await service.whenReady();

    await service.clear();

    expect(service.getAccessToken()).toBeNull();
    expect(service.getRefreshToken()).toBeNull();
    expect((await Preferences.get({ key: 'loup_access_token' })).value).toBeNull();
  });
});
