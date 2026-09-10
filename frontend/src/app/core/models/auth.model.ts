// Aligné sur backend/src/main/java/.../request et .../response (package auth).

export interface LoginRequest {
  identifiant: string; // username OU email, voir CustomUserDetailsService côté backend
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  dateNaissance?: string; // format ISO (yyyy-MM-dd), optionnel
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
}

export interface UtilisateurResponse {
  id: string;
  username: string;
  email: string;
  dateNaissance?: string;
  emailVerifie: boolean;
  personnages: unknown[]; // affiné quand le modèle Personnage sera défini
}

export interface UpdateProfilRequest {
  email?: string;
  dateNaissance?: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
