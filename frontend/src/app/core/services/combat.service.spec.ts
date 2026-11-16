import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CombatService } from './combat.service';

describe('CombatService', () => {
  const url = `${environment.apiUrl}/personnages/p1/combat`;
  let service: CombatService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CombatService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('initier', () => {
    service.initier('p1').subscribe();
    const req = backend.expectOne(url);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('recuperer', () => {
    service.recuperer('p1').subscribe();
    expect(backend.expectOne(url).request.method).toBe('GET');
  });

  it("jouerTour n'envoie que l'action (objetId à null hors OBJET)", () => {
    service.jouerTour('p1', 'ATTAQUE').subscribe();
    const req = backend.expectOne(`${url}/tour`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ action: 'ATTAQUE', objetId: null });
  });

  it("jouerTour OBJET envoie l'id de l'objet", () => {
    service.jouerTour('p1', 'OBJET', 'potion_de_soin').subscribe();
    expect(backend.expectOne(`${url}/tour`).request.body).toEqual({
      action: 'OBJET',
      objetId: 'potion_de_soin',
    });
  });
});
