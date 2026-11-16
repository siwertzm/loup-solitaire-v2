/**
 * QUAL-10 : fabriques des objets du modèle pour les tests, avec des valeurs
 * par défaut. Chaque test ne précise que ce qui compte pour lui.
 */
import {
  ChapitreResponse,
  EffetResponse,
  ObjetChapResponse,
} from '../app/core/models/chapitre.model';
import { CombatEnnemiResponse, CombatResponse } from '../app/core/models/combat.model';
import {
  CategorieObjet,
  InventaireItem,
  ObjetResume,
  PersonnageResume,
} from '../app/core/models/personnage.model';

export function unPersonnage(surcharge: Partial<PersonnageResume> = {}): PersonnageResume {
  return {
    id: 'p1',
    nom: 'Loup',
    habiliteBase: 15,
    habilite: 15,
    habiliteTemp: 0,
    enduranceMax: 25,
    enduranceActuelle: 25,
    disciplines: [],
    armeMaitrisee: null,
    chapitreActuelId: 1,
    volEnAttente: null,
    mort: false,
    inventaire: [],
    ...surcharge,
  };
}

export function unItem(
  objetId: string,
  categorie: CategorieObjet,
  quantite = 1,
  nom: string = objetId,
): InventaireItem {
  return { objetId, nom, categorie, quantite };
}

export function unObjet(
  id: string,
  categorie: CategorieObjet,
  surcharge: Partial<ObjetResume> = {},
): ObjetResume {
  return { id, nom: id, description: null, categorie, effets: [], ...surcharge };
}

export function unObjetChap(
  objetId: string,
  valeur = 1,
  optionnel = true,
  nom: string = objetId,
): ObjetChapResponse {
  return { objetId, nom, valeur, optionnel };
}

export function unEffet(type: string, surcharge: Partial<EffetResponse> = {}): EffetResponse {
  return { type, valeur: null, nom: null, conditions: [], ...surcharge };
}

export function unChapitre(surcharge: Partial<ChapitreResponse> = {}): ChapitreResponse {
  return {
    id: 1,
    text: '',
    combat: false,
    tirageHasard: null,
    ennemis: [],
    effets: [],
    liens: [],
    objets: [],
    ...surcharge,
  };
}

export function unEnnemiDeCombat(
  surcharge: Partial<CombatEnnemiResponse> = {},
): CombatEnnemiResponse {
  return {
    id: 'e1',
    nom: 'Kraan',
    habilite: 16,
    enduranceMax: 24,
    enduranceActuelle: 24,
    actif: true,
    vaincu: false,
    resistances: [],
    disciplines: [],
    ...surcharge,
  };
}

export function unCombat(surcharge: Partial<CombatResponse> = {}): CombatResponse {
  return {
    id: 'c1',
    chapitreId: 1,
    ennemis: [unEnnemiDeCombat()],
    assautsLivres: 0,
    endurancePerdue: false,
    bonusHabiliteEnAttente: 0,
    statut: 'EN_COURS',
    fuitePossible: false,
    malusMainsNues: null,
    bonusHabiliteAssaut: 0,
    dernierTour: null,
    ...surcharge,
  };
}
