import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { App as CapacitorApp } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { VersionResponse } from '../models/version.model';

/**
 * QUAL-02 : les apps installées ne se mettent pas à jour seules. Si la version
 * de l'app est plus ancienne que la version minimale de l'API, l'app affiche
 * un écran « mise à jour requise » au lieu d'appeler un contrat qui a changé.
 */
@Injectable({ providedIn: 'root' })
export class VersionService {
  private readonly http = inject(HttpClient);

  private readonly _miseAJourRequise = signal(false);
  readonly miseAJourRequise = this._miseAJourRequise.asReadonly();

  async verifier(): Promise<void> {
    try {
      const version = await this.lireVersionApp();
      if (version === null) {
        return;
      }
      const { versionMinimale } = await firstValueFrom(
        this.http.get<VersionResponse>(`${environment.apiUrl}/version`),
      );
      this._miseAJourRequise.set(estPlusAncienne(version, versionMinimale));
    } catch {
      // Pas de réseau ou serveur en veille : on ne bloque pas le joueur.
    }
  }

  /** Version installée (ex. « 1.0 ») ; null dans le navigateur, qui n'a pas de version. */
  protected async lireVersionApp(): Promise<string | null> {
    if (!Capacitor.isNativePlatform()) {
      return null;
    }
    return (await CapacitorApp.getInfo()).version;
  }
}

/**
 * Compare nombre par nombre : « 1.10 » est plus récente que « 1.9 ».
 * Une version illisible (ex. « 1.0-beta ») ne bloque jamais.
 */
export function estPlusAncienne(version: string, minimale: string): boolean {
  const a = version.split('.').map(Number);
  const b = minimale.split('.').map(Number);
  if ([...a, ...b].some(Number.isNaN)) {
    return false;
  }
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const ecart = (a[i] ?? 0) - (b[i] ?? 0);
    if (ecart !== 0) {
      return ecart < 0;
    }
  }
  return false;
}
