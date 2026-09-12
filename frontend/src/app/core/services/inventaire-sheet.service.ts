import { Injectable, signal } from '@angular/core';

import { PersonnageResume } from '../models/personnage.model';

/**
 * État global de la feuille "SAC À DOS" (voir shared/inventaire-sheet/).
 * N'importe quel composant peut appeler `ouvrir(personnageId)` pour
 * l'afficher — elle est montée une seule fois à la racine de l'appli
 * (app.ts) et se charge elle-même de récupérer les données du personnage.
 *
 * La feuille garde sa PROPRE copie de la fiche personnage (récupérée par
 * elle-même à l'ouverture) : une page qui affiche déjà ce même personnage en
 * arrière-plan (ex. ChapitrePage) n'est pas notifiée automatiquement d'un
 * ramassage/retrait fait depuis la feuille. `personnageMisAJour` sert de
 * canal de notification : toute page concernée peut l'observer (effect) et
 * rafraîchir sa propre copie quand l'id correspond.
 */
@Injectable({ providedIn: 'root' })
export class InventaireSheetService {
  private readonly _personnageId = signal<string | null>(null);
  readonly personnageId = this._personnageId.asReadonly();

  private readonly _personnageMisAJour = signal<PersonnageResume | null>(null);
  readonly personnageMisAJour = this._personnageMisAJour.asReadonly();

  ouvrir(personnageId: string): void {
    this._personnageId.set(personnageId);
  }

  fermer(): void {
    this._personnageId.set(null);
  }

  /** Appelé par la feuille après un ramassage/retrait réussi. */
  notifierMiseAJour(personnage: PersonnageResume): void {
    this._personnageMisAJour.set(personnage);
  }
}