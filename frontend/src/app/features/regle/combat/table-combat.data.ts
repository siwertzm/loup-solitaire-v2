/**
 * Copie, pour affichage dans les règles, des tables de résolution de combat
 * du backend (backend/.../service/TableCombatService.java).
 *
 * ⚠ À garder synchronisé à la main : si les tables changent côté backend,
 * reporter les mêmes valeurs ici.
 *
 * Les valeurs sont des pertes d'ENDURANCE en positif (le backend les stocke
 * en négatif). FATAL correspond au -999 du backend : mort immédiate.
 */
export const FATAL = -1;

/** Une colonne par tranche de quotient d'attaque (HABILITÉ joueur - HABILITÉ ennemi). */
export const COLONNES_QUOTIENT: readonly string[] = [
  '≤ −11',
  '−10 / −9',
  '−8 / −7',
  '−6 / −5',
  '−4 / −3',
  '−2 / −1',
  '0',
  '+1 / +2',
  '+3 / +4',
  '+5 / +6',
  '+7 / +8',
  '+9 / +10',
  '≥ +11',
];

/** Index de la colonne "0" : sélection par défaut. */
export const COLONNE_EGALITE = 6;

/** [colonne][chiffre 0-9] : ENDURANCE perdue par l'ennemi quand vous ATTAQUEZ. */
export const DEGATS_INFLIGES: readonly (readonly number[])[] = [
  [6, 0, 0, 0, 0, 1, 2, 3, 4, 5],
  [7, 0, 0, 0, 1, 2, 3, 4, 5, 6],
  [8, 0, 0, 1, 2, 3, 4, 5, 6, 7],
  [9, 0, 1, 2, 3, 4, 5, 6, 7, 8],
  [10, 1, 2, 3, 4, 5, 6, 7, 8, 9],
  [11, 2, 3, 4, 5, 6, 7, 8, 9, 10],
  [12, 3, 4, 5, 6, 7, 8, 9, 10, 11],
  [14, 4, 5, 6, 7, 8, 9, 10, 11, 12],
  [16, 5, 6, 7, 8, 9, 10, 11, 12, 14],
  [18, 6, 7, 8, 9, 10, 11, 12, 14, 16],
  [FATAL, 7, 8, 9, 10, 11, 12, 14, 16, 18],
  [FATAL, 8, 9, 10, 11, 12, 14, 16, 18, FATAL],
  [FATAL, 9, 10, 11, 12, 14, 16, 18, FATAL, FATAL],
];

/** [colonne][chiffre 0-9] : ENDURANCE que vous perdez lors de la riposte ennemie. */
export const DEGATS_SUBIS: readonly (readonly number[])[] = [
  [FATAL, 0, 3, 4, 5, 6, 7, 8, 9, FATAL],
  [FATAL, 0, 3, 4, 5, 6, 6, 7, 7, 10],
  [10, 0, 2, 3, 4, 5, 5, 6, 6, 8],
  [8, 0, 1, 2, 3, 4, 4, 5, 5, 6],
  [7, 0, 1, 1, 2, 3, 4, 4, 5, 5],
  [6, 0, 1, 1, 2, 2, 3, 4, 4, 5],
  [5, 0, 0, 1, 1, 2, 2, 3, 4, 4],
  [5, 0, 0, 1, 1, 2, 2, 3, 3, 4],
  [4, 0, 0, 1, 1, 2, 2, 2, 3, 3],
  [4, 0, 0, 0, 1, 1, 2, 2, 3, 3],
  [4, 0, 0, 0, 0, 1, 2, 2, 2, 3],
  [3, 0, 0, 0, 0, 1, 2, 2, 2, 3],
  [3, 0, 0, 0, 0, 1, 1, 2, 2, 2],
];

/** [chiffre 0-9] : part des dégâts de la riposte absorbée quand vous DÉFENDEZ (%). */
export const REDUCTION_DEFENSE: readonly number[] = [100, 25, 30, 40, 50, 60, 70, 80, 85, 90];

/** [chiffre 0-9] : bonus d'HABILITÉ gagné quand vous DÉFENDEZ. */
export const BONUS_HABILITE_DEFENSE: readonly number[] = [5, 1, 1, 2, 2, 3, 3, 3, 4, 4];

/** Ordre d'affichage des chiffres : du plus faible (1) au plus fort (9), puis le 0 exceptionnel. */
export const ORDRE_CHIFFRES: readonly number[] = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0];
