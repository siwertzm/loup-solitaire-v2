import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DisciplineResume } from '../models/personnage.model';

@Injectable({ providedIn: 'root' })
export class DisciplineService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  /** GET /disciplines — les 10 Disciplines Kaï (nom + description complète). */
  lister(): Observable<DisciplineResume[]> {
    return this.http.get<DisciplineResume[]>(`${this.base}/disciplines`);
  }
}