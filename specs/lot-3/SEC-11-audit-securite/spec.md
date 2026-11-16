# SEC-11 — Audit du 16/11/2026 : compte deviné au temps de réponse, rafraîchissements simultanés

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Haute | S | `fix-SEC-11` | Fait |

Origine : audit Claude Code du 16/11/2026 (points 1 et 2)

## Problème

1. **Compte deviné au temps de réponse** (`PasswordResetService`) : `POST /auth/forgot-password` et `POST /auth/verify-reset-code` ne faisaient le hachage bcrypt (~100 ms) que pour un email qui a un compte. En mesurant la durée de la réponse, on savait si l'adresse était inscrite. L'inscription s'en protégeait déjà (`CompteService.inscrire`), pas ces deux routes.
2. **Deux rafraîchissements simultanés du même jeton** (`RefreshTokenRepository.findByTokenHash`) : la lecture n'était pas verrouillée. Le vrai joueur et un voleur présentant le même refresh token au même instant obtenaient chacun une session neuve, sans que la réutilisation (SEC-04) soit détectée.

## Réalisé

- `forgot-password` : pour un email inconnu, un code est haché pour rien.
- `verify-reset-code` : une seule comparaison bcrypt, faite dans tous les cas (vrai code, ou hachage factice s'il n'y en a pas) ; tous les échecs prennent le même temps. Messages et limites de débit inchangés.
- `findByTokenHash` : `@Lock(PESSIMISTIC_WRITE)`. Le second rafraîchissement attend le premier, voit le jeton utilisé et révoque toutes les sessions.

## Tests

- `PasswordResetServiceTest` : hachage appelé pour un email inconnu (deux routes) ; un code épuisé ou déjà validé reste refusé même s'il est bon.
- `RefreshTokenReutilisationIT` (PostgreSQL) : deux rafraîchissements simultanés → un seul passe, toutes les sessions révoquées. Vérifié : le test échoue sans le verrou.

## Hors périmètre

Points 3 à 5 de l'audit (protections en double dans `CombatService` / `EffetChapitreService`, échafaudage répété de `EmailService`, démarrage bloqué si `Preferences` échoue) : à noter dans une tâche QUAL séparée.
