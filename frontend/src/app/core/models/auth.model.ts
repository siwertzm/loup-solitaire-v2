// Aligné sur backend/src/main/java/.../request et .../response (package auth).

export interface LoginRequest {
  identifiant: string; // username OU email, voir CustomUserDetailsService côté backend
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
}

export interface ResetCodeResponse {
  resetToken: string;
}

export interface UtilisateurResponse {
  id: string;
  username: string;
  email: string;
  emailVerifie: boolean;
  personnages: unknown[]; // affiné quand le modèle Personnage sera défini
}

export interface UpdateProfilRequest {
  email?: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}