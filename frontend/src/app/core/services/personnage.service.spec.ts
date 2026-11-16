import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PersonnageService } from './personnage.service';

// Le front n'envoie que des intentions (CLAUDE.md) : bonne URL, bonne
// méthode HTTP, corps minimal.
describe('PersonnageService', () => {
  const b = environment.apiUrl;
  let service: PersonnageService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PersonnageService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  const cas: [string, (s: PersonnageService) => Observable<unknown>, string, string, unknown][] = [
    ['lister', (s) => s.lister(), 'GET', `${b}/personnages`, null],
    ['recuperer', (s) => s.recuperer('p1'), 'GET', `${b}/personnages/p1`, null],
    ['moi', (s) => s.moi(), 'GET', `${b}/auth/me`, null],
    ['supprimer', (s) => s.supprimer('p1'), 'DELETE', `${b}/personnages/p1`, null],
    [
      'retirerObjet',
      (s) => s.retirerObjet('p1', 'repas', 2),
      'DELETE',
      `${b}/personnages/p1/objets/repas?quantite=2`,
      null,
    ],
    [
      'consommerObjet',
      (s) => s.consommerObjet('p1', 'potion'),
      'POST',
      `${b}/personnages/p1/objets/potion/consommer`,
      {},
    ],
    ['tirer', (s) => s.tirer(), 'POST', `${b}/personnages/tirage`, {}],
    [
      'creer',
      (s) => s.creer('Loup', ['CHASSE']),
      'POST',
      `${b}/personnages`,
      { nom: 'Loup', disciplines: ['CHASSE'] },
    ],
    [
      'resoudreVol',
      (s) => s.resoudreVol('p1', 'repas'),
      'POST',
      `${b}/personnages/p1/vol/repas`,
      {},
    ],
    [
      'echangerObjet',
      (s) => s.echangerObjet('p1', 'marteau', 'hache'),
      'POST',
      `${b}/personnages/p1/objets/marteau/echanger-contre/hache`,
      {},
    ],
    [
      'revenirApresDefaite',
      (s) => s.revenirApresDefaite('p1'),
      'POST',
      `${b}/personnages/p1/chapitre/revenir-apres-defaite`,
      {},
    ],
    ['ressusciter', (s) => s.ressusciter('p1'), 'POST', `${b}/personnages/p1/ressusciter`, {}],
    [
      'majCompte',
      (s) => s.majCompte({ username: 'loup' }),
      'PUT',
      `${b}/auth/me`,
      { username: 'loup' },
    ],
    [
      'renvoyerVerification',
      (s) => s.renvoyerVerification('loup@exemple.fr'),
      'POST',
      `${b}/auth/resend-verification`,
      { email: 'loup@exemple.fr' },
    ],
    ['historique', (s) => s.historique('p1'), 'GET', `${b}/personnages/p1/historique`, null],
  ];

  it.each(cas)('%s', (_nom, appel, methode, url, corps) => {
    appel(service).subscribe();

    const req = backend.expectOne(url);
    expect(req.request.method).toBe(methode);
    expect(req.request.body).toEqual(corps);
    req.flush(null);
  });
});
