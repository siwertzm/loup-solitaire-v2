import { HttpClient } from '@angular/common/http';
import {
  Injectable,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  Observable,
  tap,
} from 'rxjs';

import { environment } from '../../../environments/environment';

import {
  AuthResponse,
  LoginRequest,
  RefreshRequest,
  RegisterRequest,
  ResetCodeResponse,
  UtilisateurResponse,
} from '../models/auth.model';

import { TokenStorageService } from './token-storage.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private readonly http = inject(HttpClient);

  private readonly tokenStorage =
    inject(TokenStorageService);

  private readonly baseUrl =
    `${environment.apiUrl}/auth`;

  /*
   * Informations temporaires du parcours
   * "mot de passe oublié".
   *
   * sessionStorage est volontaire :
   * les données disparaissent lorsque la session
   * WebView / navigateur est terminée.
   */
  private readonly passwordResetEmailKey =
    'loup_password_reset_email';

  private readonly passwordResetTokenKey =
    'loup_password_reset_token';

  private readonly _utilisateur =
    signal<UtilisateurResponse | null>(null);

  readonly utilisateur =
    this._utilisateur.asReadonly();

  readonly estConnecte =
    computed(
      () => this._utilisateur() !== null,
    );

  register(
    payload: RegisterRequest,
  ): Observable<UtilisateurResponse> {

    return this.http.post<UtilisateurResponse>(
      `${this.baseUrl}/register`,
      payload,
    );
  }

  login(
    payload: LoginRequest,
  ): Observable<AuthResponse> {

    return this.http
      .post<AuthResponse>(
        `${this.baseUrl}/login`,
        payload,
      )
      .pipe(
        tap((res) =>
          this.tokenStorage.setTokens(
            res.accessToken,
            res.refreshToken,
          ),
        ),
      );
  }

  refresh(): Observable<AuthResponse> {

    const refreshToken =
      this.tokenStorage.getRefreshToken();

    const payload: RefreshRequest = {
      refreshToken: refreshToken ?? '',
    };

    return this.http
      .post<AuthResponse>(
        `${this.baseUrl}/refresh`,
        payload,
      )
      .pipe(
        tap((res) =>
          this.tokenStorage.setTokens(
            res.accessToken,
            res.refreshToken,
          ),
        ),
      );
  }

  chargerUtilisateurCourant():
    Observable<UtilisateurResponse> {

    return this.http
      .get<UtilisateurResponse>(
        `${this.baseUrl}/me`,
      )
      .pipe(
        tap((utilisateur) =>
          this._utilisateur.set(utilisateur),
        ),
      );
  }

  changePassword(
    currentPassword: string,
    newPassword: string,
  ): Observable<AuthResponse> {

    return this.http
      .put<AuthResponse>(
        `${this.baseUrl}/me/password`,
        {
          currentPassword,
          newPassword,
        },
      )
      .pipe(
        tap((res) =>
          this.tokenStorage.setTokens(
            res.accessToken,
            res.refreshToken,
          ),
        ),
      );
  }

  logout(): Observable<void> {

    const refreshToken =
      this.tokenStorage.getRefreshToken();

    return this.http
      .post<void>(
        `${this.baseUrl}/logout`,
        {
          refreshToken,
        },
      )
      .pipe(
        tap(() =>
          this.clearSessionLocale(),
        ),
      );
  }

  clearSessionLocale(): void {

    this._utilisateur.set(null);

    this.tokenStorage.clear();
  }

  /*
   * ==============================
   * MOT DE PASSE OUBLIE
   * ==============================
   */

  forgotPassword(
    email: string,
  ): Observable<void> {

    return this.http
      .post<void>(
        `${this.baseUrl}/forgot-password`,
        {
          email,
        },
      )
      .pipe(
        tap(() => {

          sessionStorage.setItem(
            this.passwordResetEmailKey,
            email,
          );

          /*
           * Une nouvelle demande invalide
           * l'ancien parcours côté frontend.
           */
          sessionStorage.removeItem(
            this.passwordResetTokenKey,
          );
        }),
      );
  }

  verifyResetCode(
    email: string,
    code: string,
  ): Observable<ResetCodeResponse> {

    return this.http
      .post<ResetCodeResponse>(
        `${this.baseUrl}/verify-reset-code`,
        {
          email,
          code,
        },
      )
      .pipe(
        tap((response) => {

          sessionStorage.setItem(
            this.passwordResetTokenKey,
            response.resetToken,
          );
        }),
      );
  }

  resetPassword(
    resetToken: string,
    newPassword: string,
  ): Observable<void> {

    return this.http
      .post<void>(
        `${this.baseUrl}/reset-password`,
        {
          resetToken,
          newPassword,
        },
      )
      .pipe(
        tap(() => {
          this.clearPasswordResetFlow();
        }),
      );
  }

  getPasswordResetEmail(): string | null {

    return sessionStorage.getItem(
      this.passwordResetEmailKey,
    );
  }

  getPasswordResetToken(): string | null {

    return sessionStorage.getItem(
      this.passwordResetTokenKey,
    );
  }

  clearPasswordResetFlow(): void {

    sessionStorage.removeItem(
      this.passwordResetEmailKey,
    );

    sessionStorage.removeItem(
      this.passwordResetTokenKey,
    );
  }
}