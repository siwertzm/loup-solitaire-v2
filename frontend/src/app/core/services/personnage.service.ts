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

  /** GET /personnages/{id} — fiche complète d'un personnage (inventaire inclus). */
  recuperer(id: string): Observable<PersonnageResume> {
    return this.http.get<PersonnageResume>(`${this.base}/personnages/${id}`);
  }

  /** GET /auth/me — profil + personnages. */
  moi(): Observable<MoiResponse> {
    return this.http.get<MoiResponse>(`${this.base}/auth/me`);
  }

  /** POST /personnages — nom, exactement 5 disciplines, et les deux jets de hasard (habileté/endurance). */
  creer(
    nom: string,
    disciplines: string[],
    hasardHabilite: number,
    hasardEndurance: number,
  ): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(`${this.base}/personnages`, {
      nom,
      disciplines,
      hasardHabilite,
      hasardEndurance,
    });
  }

  /** PUT /auth/me — met à jour username/email (email → re-vérification nécessaire). */
  majCompte(payload: { username: string; email: string }): Observable<MoiResponse> {
    return this.http.put<MoiResponse>(`${this.base}/auth/me`, payload);
  }
 
  /** POST /auth/resend-verification — renvoie le lien de vérification à l'email donné. */
  renvoyerVerification(email: string): Observable<void> {
    return this.http.post<void>(`${this.base}/auth/resend-verification`, { email });
  }
}