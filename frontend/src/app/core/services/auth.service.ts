import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RefreshRequest,
  RegisterRequest,
  UtilisateurResponse,
} from '../models/auth.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  /** Utilisateur courant, hydraté via /auth/me après login ou au démarrage de l'app. */
  private readonly _utilisateur = signal<UtilisateurResponse | null>(null);
  readonly utilisateur = this._utilisateur.asReadonly();
  readonly estConnecte = computed(() => this._utilisateur() !== null);

  register(payload: RegisterRequest): Observable<UtilisateurResponse> {
    return this.http.post<UtilisateurResponse>(`${this.baseUrl}/register`, payload);
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, payload)
      .pipe(tap((res) => this.tokenStorage.setTokens(res.accessToken, res.refreshToken)));
  }

  /** Utilisé par l'intercepteur quand l'accessToken a expiré (401). */
  refresh(): Observable<AuthResponse> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    const payload: RefreshRequest = { refreshToken: refreshToken ?? '' };
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/refresh`, payload)
      .pipe(tap((res) => this.tokenStorage.setTokens(res.accessToken, res.refreshToken)));
  }

  chargerUtilisateurCourant(): Observable<UtilisateurResponse> {
    return this.http
      .get<UtilisateurResponse>(`${this.baseUrl}/me`)
      .pipe(tap((utilisateur) => this._utilisateur.set(utilisateur)));
  }

  /** Révoque les autres sessions côté serveur ; renvoie un nouveau couple de tokens pour rester connecté ici. */
  changePassword(currentPassword: string, newPassword: string): Observable<AuthResponse> {
    return this.http
      .put<AuthResponse>(`${this.baseUrl}/me/password`, { currentPassword, newPassword })
      .pipe(tap((res) => this.tokenStorage.setTokens(res.accessToken, res.refreshToken)));
  }

  logout(): Observable<void> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    return this.http
      .post<void>(`${this.baseUrl}/logout`, { refreshToken })
      .pipe(tap(() => this.clearSessionLocale()));
  }

  /** Nettoyage local seul (ex. refresh token déjà invalide côté serveur). */
  clearSessionLocale(): void {
    this._utilisateur.set(null);
    this.tokenStorage.clear();
  }
}