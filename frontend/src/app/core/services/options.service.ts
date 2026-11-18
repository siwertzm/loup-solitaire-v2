import { inject, Injectable, signal } from '@angular/core';
import { Preferences } from '@capacitor/preferences';
import { TranslateService } from '@ngx-translate/core';

export type TailleTexte = 'PETITE' | 'NORMALE' | 'GRANDE';
export type Langue = 'fr' | 'en';

const CLE_TAILLE_TEXTE = 'loup_taille_texte';
const CLE_LANGUE = 'loup_langue';
export const LANGUES: Langue[] = ['fr', 'en'];

/** QUAL-09 : langue de l'appareil si elle est proposée (« en-GB » → en), sinon français. */
export function langueParDefaut(langueAppareil: string | undefined): Langue {
  const code = langueAppareil?.slice(0, 2).toLowerCase();
  return LANGUES.find((l) => l === code) ?? 'fr';
}

/** Multiplicateur de la taille de base (16px) pour chaque taille proposée. */
export const ECHELLES_TEXTE: Record<TailleTexte, number> = {
  PETITE: 0.9,
  NORMALE: 1,
  GRANDE: 1.15,
};

/**
 * QUAL-15 / QUAL-09 : paramètres de l'app (taille du texte, langue), gardés sur l'appareil seulement
 * (Capacitor Preferences). La taille du texte est appliquée via la variable
 * CSS --echelle-texte de :root (voir styles.scss).
 */
@Injectable({ providedIn: 'root' })
export class OptionsService {
  private readonly translate = inject(TranslateService);

  readonly tailleTexte = signal<TailleTexte>('NORMALE');
  readonly langue = signal<Langue>('fr');

  /** Relit les paramètres enregistrés et les applique (démarrage de l'app). */
  async charger(): Promise<void> {
    const { value } = await Preferences.get({ key: CLE_TAILLE_TEXTE });
    this.appliquer(value && value in ECHELLES_TEXTE ? (value as TailleTexte) : 'NORMALE');
    const langue = (await Preferences.get({ key: CLE_LANGUE })).value;
    this.appliquerLangue(
      LANGUES.find((l) => l === langue) ?? langueParDefaut(globalThis.navigator?.language),
    );
  }

  async definirLangue(langue: Langue): Promise<void> {
    this.appliquerLangue(langue);
    await Preferences.set({ key: CLE_LANGUE, value: langue });
  }

  private appliquerLangue(langue: Langue): void {
    this.langue.set(langue);
    this.translate.use(langue);
    document.documentElement.lang = langue;
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
