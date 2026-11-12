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
  emailEnAttente: string | null;
  personnages: unknown[]; // affiné quand le modèle Personnage sera défini
}

export interface UpdateProfilRequest {
  username?: string;
  email?: string;
  /** SEC-06 : exigé par le serveur seulement si l'e-mail change. */
  motDePasseActuel?: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}