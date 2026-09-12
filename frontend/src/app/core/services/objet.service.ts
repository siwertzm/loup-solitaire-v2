import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ObjetResume } from '../models/personnage.model';

@Injectable({ providedIn: 'root' })
export class ObjetService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  lister(): Observable<ObjetResume[]> {
    return this.http.get<ObjetResume[]>(`${this.base}/objets`);
  }
}