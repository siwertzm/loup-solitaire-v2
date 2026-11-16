import { Injectable, signal } from '@angular/core';
import { Preferences } from '@capacitor/preferences';

export type TailleTexte = 'PETITE' | 'NORMALE' | 'GRANDE';

const CLE_TAILLE_TEXTE = 'loup_taille_texte';

/** Multiplicateur de la taille de base (16px) pour chaque taille proposée. */
export const ECHELLES_TEXTE: Record<TailleTexte, number> = {
  PETITE: 0.9,
  NORMALE: 1,
  GRANDE: 1.15,
};

/**
 * QUAL-15 : paramètres de l'app, gardés sur l'appareil seulement
 * (Capacitor Preferences). La taille du texte est appliquée via la variable
 * CSS --echelle-texte de :root (voir styles.scss).
 */
@Injectable({ providedIn: 'root' })
export class OptionsService {
  readonly tailleTexte = signal<TailleTexte>('NORMALE');

  /** Relit les paramètres enregistrés et les applique (démarrage de l'app). */
  async charger(): Promise<void> {
    const { value } = await Preferences.get({ key: CLE_TAILLE_TEXTE });
    this.appliquer(value && value in ECHELLES_TEXTE ? (value as TailleTexte) : 'NORMALE');
  }

  async definirTailleTexte(taille: TailleTexte): Promise<void> {
    this.appliquer(taille);
    await Preferences.set({ key: CLE_TAILLE_TEXTE, value: taille });
  }

  private appliquer(taille: TailleTexte): void {
    this.tailleTexte.set(taille);
    document.documentElement.style.setProperty('--echelle-texte', String(ECHELLES_TEXTE[taille]));
    // Pour les rares ajustements de mise en page propres à une taille.
    document.documentElement.dataset['tailleTexte'] = taille;
  }
}
