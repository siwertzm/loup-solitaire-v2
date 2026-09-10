export interface PersonnageResume {
  id: string;
  nom: string;
  habilete: number;
  habileteTemporaire: number;
  endurance: number;
  enduranceMax: number;
  or: number;
  chapitreCourantId: number;
  mort: boolean;
}

export interface MoiResponse {
  username: string;
  email: string;
  emailVerifie: boolean;
  personnages: PersonnageResume[];
}
