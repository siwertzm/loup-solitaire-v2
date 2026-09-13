/**
 * Miroir exact des DTOs backend (voir CombatResponse, CombatEnnemiResponse,
 * ResultatTourResponse côté Java) : le serveur fait autorité sur l'issue de
 * chaque tour (tirages, table de résolution), le front ne fait qu'afficher.
 */

export type StatutCombat = 'EN_COURS' | 'VICTOIRE' | 'DEFAITE' | 'FUITE' | 'INTERROMPU';
export type ActionCombat = 'ATTAQUE' | 'DEFENSE' | 'OBJET' | 'FUITE';

export interface CombatEnnemiResponse {
  id: string;
  nom: string;
  habilite: number;
  enduranceMax: number;
  enduranceActuelle: number;
  actif: boolean;
  vaincu: boolean;
}

/**
 * Détail du tour qui vient d'être joué. Champs à null selon l'action jouée
 * (voir commentaire du record Java) :
 * - rapportAttaque/tirageAttaque/degatsInfliges : uniquement ATTAQUE.
 * - rapportRiposte/tirageRiposte/degatsSubisBruts/degatsSubis : jamais pour FUITE.
 * - tirageDefense/reductionPourcent/bonusHabiliteObtenu : uniquement DEFENSE.
 */
export interface ResultatTourResponse {
  action: string;
  rapportAttaque: number | null;
  tirageAttaque: number | null;
  degatsInfliges: number | null;
  rapportRiposte: number | null;
  tirageRiposte: number | null;
  degatsSubisBruts: number | null;
  degatsSubis: number | null;
  tirageDefense: number | null;
  reductionPourcent: number | null;
  bonusHabiliteObtenu: number | null;
}

export interface CombatResponse {
  id: string;
  chapitreId: number;
  ennemis: CombatEnnemiResponse[];
  assautsLivres: number;
  endurancePerdue: boolean;
  bonusHabiliteEnAttente: number;
  statut: StatutCombat;
  fuitePossible: boolean;
  /** Null sur GET/POST /combat ; rempli uniquement par POST /combat/tour. */
  dernierTour: ResultatTourResponse | null;
}