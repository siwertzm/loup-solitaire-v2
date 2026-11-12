import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { VersionService, estPlusAncienne } from './version.service';

describe('estPlusAncienne', () => {
  it.each([
    ['1.0', '1.0', false],
    ['1.0', '1.1', true],
    ['1.1', '1.0', false],
    ['1.9', '1.10', true],
    ['1.10', '1.9', false],
    ['1', '1.0.1', true],
    ['2.0', '1.9.9', false],
    ['1.0-beta', '2.0', false],
  ])('%s face au minimum %s -> %s', (version, minimale, attendu) => {
    expect(estPlusAncienne(version, minimale)).toBe(attendu);
  });
});

// Version installee simulee : Capacitor n'existe pas hors de l'app native.
@Injectable()
class VersionServiceDeTest extends VersionService {
  versionApp: string | null = '1.0';
  protected override async lireVersionApp(): Promise<string | null> {
    return this.versionApp;
  }
}

describe('VersionService', () => {
  let service: VersionServiceDeTest;
  let backend: HttpTestingController;
  const urlVersion = `${environment.apiUrl}/version`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: VersionService, useClass: VersionServiceDeTest },
      ],
    });
    service = TestBed.inject(VersionService) as VersionServiceDeTest;
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  async function verifierAvec(reponse: () => void): Promise<void> {
    const verification = service.verifier();
    await Promise.resolve();
    reponse();
    await verification;
  }

  it('demande la mise a jour si l app est plus ancienne que le minimum', async () => {
    await verifierAvec(() => backend.expectOne(urlVersion).flush({ versionMinimale: '1.1' }));
    expect(service.miseAJourRequise()).toBe(true);
  });

  it('laisse jouer si l app est a jour', async () => {
    await verifierAvec(() => backend.expectOne(urlVersion).flush({ versionMinimale: '1.0' }));
    expect(service.miseAJourRequise()).toBe(false);
  });

  it('ne bloque pas si le serveur ne repond pas', async () => {
    await verifierAvec(() =>
      backend.expectOne(urlVersion).flush(null, { status: 503, statusText: 'Unavailable' }),
    );
    expect(service.miseAJourRequise()).toBe(false);
  });

  it('ne verifie rien dans le navigateur (pas de version installee)', async () => {
    service.versionApp = null;
    await service.verifier();
    backend.expectNone(urlVersion);
    expect(service.miseAJourRequise()).toBe(false);
  });
});
