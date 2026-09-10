import { Injectable } from '@angular/core';
import { Preferences } from '@capacitor/preferences';

const ACCESS_TOKEN_KEY = 'loup_access_token';
const REFRESH_TOKEN_KEY = 'loup_refresh_token';

/**
 * Abstraction du stockage des tokens.
 * Utilise Capacitor Preferences : fonctionne à l'identique sur web (fallback
 * localStorage en interne) et sur natif (Keychain/SharedPreferences via Capacitor).
 *
 * Un cache mémoire (`accessTokenSnapshot`) permet à l'intercepteur HTTP de lire
 * le token de façon synchrone (Preferences.get est asynchrone).
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private accessTokenSnapshot: string | null = null;
  private refreshTokenSnapshot: string | null = null;
  private ready: Promise<void>;

  constructor() {
    this.ready = this.hydrate();
  }

  private async hydrate(): Promise<void> {
    const [access, refresh] = await Promise.all([
      Preferences.get({ key: ACCESS_TOKEN_KEY }),
      Preferences.get({ key: REFRESH_TOKEN_KEY }),
    ]);
    this.accessTokenSnapshot = access.value;
    this.refreshTokenSnapshot = refresh.value;
  }

  /** À appeler avant toute lecture critique au démarrage (ex. auth guard). */
  whenReady(): Promise<void> {
    return this.ready;
  }

  getAccessToken(): string | null {
    return this.accessTokenSnapshot;
  }

  getRefreshToken(): string | null {
    return this.refreshTokenSnapshot;
  }

  async setTokens(accessToken: string, refreshToken: string): Promise<void> {
    this.accessTokenSnapshot = accessToken;
    this.refreshTokenSnapshot = refreshToken;
    await Promise.all([
      Preferences.set({ key: ACCESS_TOKEN_KEY, value: accessToken }),
      Preferences.set({ key: REFRESH_TOKEN_KEY, value: refreshToken }),
    ]);
  }

  async clear(): Promise<void> {
    this.accessTokenSnapshot = null;
    this.refreshTokenSnapshot = null;
    await Promise.all([
      Preferences.remove({ key: ACCESS_TOKEN_KEY }),
      Preferences.remove({ key: REFRESH_TOKEN_KEY }),
    ]);
  }
}
