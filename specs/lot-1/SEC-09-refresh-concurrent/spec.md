# SEC-09 — Déconnexion forcée quand plusieurs requêtes renouvellent la session en même temps

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 1 | Critique | S | `fix-SEC-09` | À faire |

Origine : Audit Claude Code du 11/11/2026 (nouveau)

## Problème

Le jeton d'accès dure 15 minutes. Quand il expire, `auth.interceptor.ts` appelle `AuthService.refresh()` **pour chaque requête en échec**, sans partager l'appel.

La page chapitre lance 4 requêtes en parallèle (`chapitre.page.ts`, `chargerToutesLesDonnees`), la page combat 2. Après 15 minutes de lecture, les 4 reçoivent un 401 et envoient 4 refresh avec **le même** refresh token. Le premier réussit ; le deuxième présente un jeton déjà révoqué, et `RefreshTokenService.validerEtPivoter` croit à un vol : il révoque **toutes** les sessions (`RevocationDesSessions`, y compris celle qui vient d'être créée). Le joueur est renvoyé sur l'écran de connexion.

Cas très fréquent dans un livre-jeu (on lit longtemps un chapitre). Visible dès aujourd'hui sur TestFlight.

## À faire

- Dans `frontend/src/app/core/services/auth.service.ts`, partager un seul refresh en cours :
  champ `private refreshEnCours$?: Observable<AuthResponse>`, créé avec `shareReplay(1)` et remis à `undefined` dans `finalize`.
- L'intercepteur ne change pas : il appelle toujours `authService.refresh()`.
- Ne rien changer côté serveur : la détection de réutilisation (SEC-04) doit rester stricte.

## Critères d'acceptation

- Quatre requêtes qui reçoivent un 401 en même temps déclenchent **un seul** `POST /auth/refresh`, puis sont toutes rejouées avec le nouveau jeton.
- Si le refresh échoue, la session locale est vidée une seule fois et l'app va sur `/auth/login`.
- Test manuel TestFlight : rester 16 minutes sur un chapitre puis avancer → pas de déconnexion.

## Tests

- `auth.interceptor.spec.ts` (premier test frontend, voir QUAL-06) : deux requêtes en 401 simultanées → `HttpTestingController` ne voit qu'un appel à `/auth/refresh`.

## Fichiers concernés

`frontend/src/app/core/services/auth.service.ts`, `frontend/src/app/core/interceptors/auth.interceptor.ts`
