import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ChapitreService } from './chapitre.service';

describe('ChapitreService', () => {
  const b = environment.apiUrl;
  let service: ChapitreService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ChapitreService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  const cas: [string, (s: ChapitreService) => Observable<unknown>, string, string, unknown][] = [
    [
      'getChapitreCourant',
      (s) => s.getChapitreCourant('p1'),
      'GET',
      `${b}/personnages/p1/chapitre`,
      null,
    ],
    [
      'avancerVersChapitre',
      (s) => s.avancerVersChapitre('p1', 85),
      'POST',
      `${b}/personnages/p1/chapitre/85`,
      {},
    ],
    [
      'revenirApresDefaite',
      (s) => s.revenirApresDefaite('p1'),
      'POST',
      `${b}/personnages/p1/chapitre/revenir-apres-defaite`,
      {},
    ],
    [
      'ramasserObjet',
      (s) => s.ramasserObjet('p1', 'epee'),
      'POST',
      `${b}/personnages/p1/objets/epee`,
      {},
    ],
  ];

  it.each(cas)('%s', (_nom, appel, methode, url, corps) => {
    appel(service).subscribe();

    const req = backend.expectOne(url);
    expect(req.request.method).toBe(methode);
    expect(req.request.body).toEqual(corps);
    req.flush(null);
  });
});
