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

// Aligné sur backend/.../response/DisciplineResponse.java (GET /disciplines).
export interface DisciplineResume {
  id: IdDiscipline;
  nom: string;
  description: string;
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
  inventaire: unknown[]; // affiné quand InventaireItemResponse sera modélisé
}

export interface MoiResponse {
  username: string;
  email: string;
  emailVerifie: boolean;
  personnages: PersonnageResume[];
}