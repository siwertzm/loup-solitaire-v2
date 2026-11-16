// Aligné sur backend/src/main/java/.../response/PersonnageResponse.java

// Aligné sur backend/.../model/enums/IdDiscipline.java — les 10 Disciplines Kaï.
export type IdDiscipline =
  | 'CAMOUFLAGE'
  | 'CHASSE'
  | 'SIXIEME_SENS'
  | 'ORIENTATION'
  | 'GUERISON'
  | 'MAITRISE_ARMES'
  | 'BOUCLIER_PSYCHIQUE'
  | 'PUISSANCE_PSYCHIQUE'
  | 'COMMUNICATION_ANIMALE'
  | 'MAITRISE_MATIERE';

export const NB_DISCIPLINES_A_CHOISIR = 5;

// Aligné sur backend/.../response/TirageResponse.java (POST /personnages/tirage).
// Tirage fait par le serveur : chiffres 0-9 et totaux (10 + / 20 +).
export interface TirageCreation {
  hasardHabilite: number;
  hasardEndurance: number;
  habilite: number;
  endurance: number;
}

// Aligné sur backend/.../response/DisciplineResponse.java (GET /disciplines).
export interface DisciplineResume {
  id: IdDiscipline;
  nom: string;
  description: string;
}

// Aligné sur backend/.../response/ObjetResponse.java (GET /objets).
export interface EffetObjet {
  type: 'ENDURANCE' | 'HABILITE';
  valeur: number;
}

export interface ObjetResume {
  id: string;
  nom: string;
  description: string | null;
  categorie: CategorieObjet;
  effets: EffetObjet[];
}

// Aligné sur backend/.../response/InventaireItemResponse.java — structure
// PLATE (objetId/nom/categorie/quantite), pas d'objet imbriqué.
export type CategorieObjet = 'ARME' | 'OBJET' | 'OBJETS_SPECIAUX' | 'REPAS' | 'BOURSE';

/**
 * REGLE-12 : le Sac à Dos (Objet Spécial) porte les objets et repas.
 * Aligné sur InventaireService.OBJET_SAC_A_DOS (backend).
 */
export const OBJET_SAC_A_DOS = 'sac_a_dos';

/**
 * QUAL-12 : bonus d'ENDURANCE des Objets Spéciaux possédés (Casque +2, Cotte
 * de mailles +4). Effet passif, déjà inclus par le serveur dans enduranceMax
 * et enduranceActuelle (voir ObjetService côté backend).
 */
export function bonusEnduranceObjetsSpeciaux(
  inventaire: { objetId: string; categorie: CategorieObjet; quantite: number }[],
  catalogue: ObjetResume[],
): number {
  return inventaire
    .filter((i) => i.categorie === 'OBJETS_SPECIAUX' && i.quantite > 0)
    .flatMap((i) => catalogue.find((o) => o.id === i.objetId)?.effets ?? [])
    .filter((e) => e.type === 'ENDURANCE')
    .reduce((total, e) => total + e.valeur, 0);
}

/** Sans Sac à Dos, aucun objet ni repas ne peut être porté. */
export function possedeSacADos(inventaire: { objetId: string }[]): boolean {
  return inventaire.some((i) => i.objetId === OBJET_SAC_A_DOS);
}

export type CategorieEffet = 'ENDURANCE' | 'HABILITE' | 'ECHANGE' | 'REPAS' | 'VOL';

export interface InventaireItem {
  objetId: string;
  nom: string;
  categorie: CategorieObjet;
  quantite: number;
}

export interface PersonnageResume {
  id: string;
  nom: string;
  habiliteBase: number;
  habilite: number; // valeur effective courante (habiliteBase ajustée par les armes)
  habiliteTemp: number;
  enduranceMax: number;
  enduranceActuelle: number;
  disciplines: string[];
  armeMaitrisee: string | null;
  chapitreActuelId: number | null;
  volEnAttente: string | null;
  mort: boolean;
  inventaire: InventaireItem[]; // affiné quand InventaireItemResponse sera modélisé
}

export interface MoiResponse {
  username: string;
  email: string;
  emailVerifie: boolean;
  /** SEC-06 : nouvel e-mail en attente de confirmation (null sinon). L'e-mail
   * ci-dessus reste celui du compte tant que le lien n'est pas cliqué. */
  emailEnAttente: string | null;
  /** ISO 8601 (Instant backend). Null pour les comptes créés avant
   * l'ajout de ce champ (voir Utilisateur.dateCreation côté backend). */
  dateCreation: string | null;
  personnages: PersonnageResume[];
}

/**
 * Une arrivée du journal (GET /personnages/{id}/historique) : le chapitre et le
 * début de son texte. `extrait` est déjà du texte brut (sans balisage), prévu
 * pour tenir sur une ligne ; vide si le texte du chapitre est introuvable.
 */
export interface ChapitreParcouru {
  chapitreId: number;
  extrait: string;
  avecCombat: boolean;
  avecEffets: boolean;
  avecObjets: boolean;
  mort: boolean;
}