export interface CondResponse {
  type: string;
  targetId: string | null;
  valeur: string | null;
}

export interface EnnemiChapitreResponse {
  id: string;
  nom: string;
  habilite: number;
  endurance: number;
}

export interface EffetResponse {
  type: string;
  valeur: number | null;
  conditions: CondResponse[];
}

export interface LienResponse {
  chapitreCibleId: number;
  disponible: boolean;
  conditions: CondResponse[];
}

export interface ObjetChapResponse {
  objetId: string;
  nom: string;
  valeur: number;
  optionnel: boolean;
}

export interface ChapitreResponse {
  id: number;
  text: string;
  combat: boolean;
  tirageHasard: number | null;
  ennemis: EnnemiChapitreResponse[];
  effets: EffetResponse[];
  liens: LienResponse[];
  objets: ObjetChapResponse[];
}
