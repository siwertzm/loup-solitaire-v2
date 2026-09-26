import { environment } from '../../environments/environment';

/**
 * RGPD-04 : pages légales publiques, servies par le backend
 * (backend/src/main/resources/static/legal). Même adresse que dans les
 * emails et sur les stores.
 */
export const LIENS_LEGAUX = {
  confidentialite: `${environment.apiUrl}/legal/confidentialite.html`,
  cgu: `${environment.apiUrl}/legal/cgu.html`,
} as const;