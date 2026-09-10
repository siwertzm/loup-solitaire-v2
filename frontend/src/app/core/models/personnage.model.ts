// Aligné sur backend/src/main/java/.../response/PersonnageResponse.java

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