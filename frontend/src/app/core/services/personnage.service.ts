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

  /** DELETE /personnages/{id} — supprime définitivement un personnage. */
  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/personnages/${id}`);
  }

  /**
   * DELETE /personnages/{id}/objets/{objetId}?quantite=N
   * Retire N exemplaires (1 par défaut) d'un objet possédé. Recalcule
   * l'HABILITE côté backend si c'était une arme.
   */
  retirerObjet(personnageId: string, objetId: string, quantite = 1): Observable<PersonnageResume> {
    return this.http.delete<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/objets/${objetId}?quantite=${quantite}`,
    );
  }

  /**
   * POST /personnages/{id}/objets/{objetId}/consommer
   * Applique l'effet de l'objet (endurance/habilite, voir ObjetService
   * backend) PUIS le retire de l'inventaire — contrairement à retirerObjet,
   * modifie les stats du personnage. Réservé à la catégorie OBJET.
   */
  consommerObjet(personnageId: string, objetId: string): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/objets/${objetId}/consommer`,
      {},
    );
  }

  /** POST /personnages — nom, exactement 5 disciplines, et les deux jets de hasard (habilite/endurance). */
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

  /**
   * POST /personnages/{id}/vol/{objetId}
   * Résout un vol en attente (Effet VOL, valeur=1) : le joueur choisit quel
   * objet/arme perdre parmi ceux autorisés par la portée du vol
   * (`Personnage.volEnAttente` : "ARME" ou "TOUT").
   */
  resoudreVol(personnageId: string, objetId: string): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/vol/${objetId}`,
      {},
    );
  }

  /**
   * POST /personnages/{id}/objets/{objetAAjouterId}/echanger-contre/{objetARetirerId}
   * Échange volontaire d'objet, uniquement proposé explicitement par le
   * chapitre courant (Effet ECHANGE). Cas d'usage unique dans ce tome :
   * chapitre 307, le Marteau de Guerre de l'ermite contre une arme déjà
   * possédée. Le backend vérifie que les deux objets sont de même catégorie.
   */
  echangerObjet(
    personnageId: string,
    objetAAjouterId: string,
    objetARetirerId: string,
  ): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/objets/${objetAAjouterId}/echanger-contre/${objetARetirerId}`,
      {},
    );
  }

  /**
   * POST /personnages/{id}/chapitre/revenir-apres-defaite
   * Suite à une DEFAITE en combat (endurance à 0 pendant un tour) : renvoie
   * le personnage au dernier chapitre "sûr" (coûte 1 pièce premium côté
   * backend). Distinct de ressusciter() : ne change pas de chapitre.
   */
  revenirApresDefaite(personnageId: string): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/chapitre/revenir-apres-defaite`,
      {},
    );
  }

  /**
   * POST /personnages/{id}/ressusciter
   * Suite à une mort HORS combat (chapitre de mort narrative, ou perte
   * d'endurance via un effet/repas de chapitre — voir Personnage.mort) :
   * restaure l'endurance au maximum et repasse mort à false, mais reste
   * sur le MÊME chapitre (contrairement à revenirApresDefaite, réservé à
   * la mort en combat). Coûte 1 pièce premium côté backend. Le backend
   * refuse (400) si une défaite de combat est en attente sur ce chapitre :
   * il faut alors passer par revenirApresDefaite à la place.
   */
  ressusciter(personnageId: string): Observable<PersonnageResume> {
    return this.http.post<PersonnageResume>(
      `${this.base}/personnages/${personnageId}/ressusciter`,
      {},
    );
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