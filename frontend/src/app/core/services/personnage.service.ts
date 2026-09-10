import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { MoiResponse, PersonnageResume } from '../models/personnage.model';

@Injectable({ providedIn: 'root' })
export class PersonnageService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  /** GET /personnages — liste des personnages de l'utilisateur connecté. */
  lister(): Observable<PersonnageResume[]> {
    return this.http.get<PersonnageResume[]>(`${this.base}/personnages`);
  }

  /** GET /auth/me — profil + personnages. */
  moi(): Observable<MoiResponse> {
    return this.http.get<MoiResponse>(`${this.base}/auth/me`);
  }

  /** POST /personnages — nom + exactement 5 disciplines. */
  creer(nom: string, disciplines: string[]): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(`${this.base}/personnages`, { nom, disciplines });
  }
}
