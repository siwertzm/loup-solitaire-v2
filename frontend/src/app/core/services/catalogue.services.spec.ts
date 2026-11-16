import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DisciplineService } from './discipline.service';
import { ObjetService } from './objet.service';

describe('Services de catalogue', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('ObjetService.lister : GET /objets', () => {
    TestBed.inject(ObjetService).lister().subscribe();
    expect(backend.expectOne(`${environment.apiUrl}/objets`).request.method).toBe('GET');
  });

  it('DisciplineService.lister : GET /disciplines', () => {
    TestBed.inject(DisciplineService).lister().subscribe();
    expect(backend.expectOne(`${environment.apiUrl}/disciplines`).request.method).toBe('GET');
  });
});
