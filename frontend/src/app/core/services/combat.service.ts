import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ActionCombat, CombatResponse } from '../models/combat.model';

@Injectable({ providedIn: 'root' })
export class CombatService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  /**
   * POST /personnages/{id}/combat — démarre le combat du chapitre courant,
   * ou renvoie celui déjà EN_COURS (idempotent : rouvrir l'écran ne relance
   * pas le combat à zéro). 400 si le chapitre courant n'est pas combat=true.
   */
  initier(personnageId: string): Observable<CombatResponse> {
    return this.http.post<CombatResponse>(`${this.base}/personnages/${personnageId}/combat`, {});
  }

  /**
   * GET /personnages/{id}/combat — état du combat en cours (ou du dernier
   * résolu) sur le chapitre actuel. Contrairement à initier(), ne crée
   * rien : 404 si aucun combat n'a encore été initié sur ce chapitre.
   */
  recuperer(personnageId: string): Observable<CombatResponse> {
    return this.http.get<CombatResponse>(`${this.base}/personnages/${personnageId}/combat`);
  }

  /**
   * POST /personnages/{id}/combat/tour — joue un tour. objetId requis
   * uniquement pour action="OBJET" (id d'un objet consommable possédé).
   * Le serveur fait autorité sur le résultat : le corps ne contient que le
   * choix d'action, jamais un résultat calculé côté client.
   */
  jouerTour(personnageId: string, action: ActionCombat, objetId?: string): Observable<CombatResponse> {
    return this.http.post<CombatResponse>(`${this.base}/personnages/${personnageId}/combat/tour`, {
      action,
      objetId: objetId ?? null,
    });
  }
}