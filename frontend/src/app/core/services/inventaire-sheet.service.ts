import { Injectable, signal } from '@angular/core';

/**
 * État global de la feuille "SAC À DOS" (voir shared/inventaire-sheet/).
 * N'importe quel composant peut appeler `ouvrir(personnageId)` pour
 * l'afficher — elle est montée une seule fois à la racine de l'appli
 * (app.ts) et se charge elle-même de récupérer les données du personnage.
 */
@Injectable({ providedIn: 'root' })
export class InventaireSheetService {
  private readonly _personnageId = signal<string | null>(null);
  readonly personnageId = this._personnageId.asReadonly();

  ouvrir(personnageId: string): void {
    this._personnageId.set(personnageId);
  }

  fermer(): void {
    this._personnageId.set(null);
  }
}