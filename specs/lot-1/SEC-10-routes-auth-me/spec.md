# SEC-10 — `/auth/me` répond 500 au lieu de 401 quand le jeton a expiré

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 1 | Haute | S | `fix-SEC-10` | Fait |

Origine : Audit Claude Code du 11/11/2026 (nouveau)

## Problème

`SecurityConfig` ouvre tout `/auth/**` (`permitAll`). Or les routes du compte sont aussi sous `/auth` : `GET /auth/me`, `PUT /auth/me`, `PUT /auth/me/password`, `DELETE /auth/me`.

Sans jeton valide (jeton expiré après 15 minutes, ou absent), la requête passe quand même. `@AuthenticationPrincipal UtilisateurConnecte connecte` vaut alors `null`, et `CompteService.recupererUtilisateur` lève une `NullPointerException` : réponse **500**.

Conséquence côté app : `auth.interceptor.ts` ne renouvelle la session que sur un **401**. Après 15 minutes d'inactivité, l'écran Profil affiche « Impossible de charger votre profil. » au lieu de renouveler le jeton. Le commentaire de l'intercepteur (« /auth/** (sauf /auth/me) ») décrit le comportement voulu, pas le comportement réel.

Les tests passent car `AuthControllerTest` authentifie toujours l'utilisateur avant d'appeler `/auth/me`.

## À faire

- `backend/.../config/SecurityConfig.java` : déclarer `/auth/me` et `/auth/me/**` comme `authenticated()` **avant** la règle `/auth/**` (Spring applique la première règle qui correspond).
- Mettre à jour la phrase de `backend/README.md` (« Toutes les routes sont protégées… sauf `/auth/**` ») : préciser « sauf `/auth/me` ».

## Critères d'acceptation

- `GET`, `PUT`, `DELETE /auth/me` et `PUT /auth/me/password` sans jeton → 401 JSON (`JwtAuthenticationEntryPoint`), jamais 500.
- Les autres routes `/auth/**` (login, register, refresh…) restent publiques.
- Test manuel : rester 16 minutes sur l'écran Profil puis le rouvrir → profil affiché, sans erreur.

## Tests

- `SecurityConfigTest.lesRoutesDuCompteSansJetonRepondent401` : les 4 routes ci-dessus sans en-tête `Authorization` → 401.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/config/SecurityConfig.java`, `backend/README.md`, `backend/src/test/java/com/loupsolitaire/backend/config/SecurityConfigTest.java`
