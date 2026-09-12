import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ChapitreResponse } from '../models/chapitre.model';
import { PersonnageResume } from '../models/personnage.model';

@Injectable({ providedIn: 'root' })
export class ChapitreService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  /**
   * GET /personnages/{id}/chapitre
   * Récupère le chapitre courant du personnage (texte, liens, ennemis, objets, effets, tirage d'hasard).
   */
  getChapitreCourant(personnageId: string): Observable<ChapitreResponse> {
    return this.http.get<ChapitreResponse>(`${this.base}/personnages/${personnageId}/chapitre`);
  }

  /**
   * POST /personnages/{id}/chapitre/{chapitreCibleId}
   * Avance le personnage vers un chapitre cible s'il est accessible.
   */
  avancerVersChapitre(personnageId: string, chapitreCibleId: number): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/chapitre/${chapitreCibleId}`,
      {}
    );
  }

  /**
   * POST /personnages/{id}/chapitre/revenir-apres-defaite
   * Permet de revenir au chapitre précédent après une défaite en combat (coûte 1 Pièce Premium).
   */
  revenirApresDefaite(personnageId: string): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/chapitre/revenir-apres-defaite`,
      {}
    );
  }
}
