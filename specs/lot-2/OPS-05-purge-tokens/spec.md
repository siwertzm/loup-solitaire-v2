# OPS-05 — Purge quotidienne des tokens expirés et des comptes non vérifiés

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `feature-OPS-05` | Fait |

Origine : Cahier des charges (OPS-05) + audit Claude Code du 11/11/2026

## Problème

Aucune tâche planifiée (`@Scheduled`) n'existe. Chaque renouvellement de session crée une ligne `refresh_token` (environ 4 par heure de jeu et par joueur), jamais supprimée. Même chose pour les tokens de vérification et de réinitialisation, et pour les comptes jamais vérifiés. La table grossit sans fin sur une base Render à stockage limité.

## À faire

- `@EnableScheduling` et un job quotidien (`@Scheduled(cron = ...)`) :
  - supprimer les refresh tokens **expirés depuis plus de 7 jours** (garder les révoqués non expirés : ils servent à détecter la réutilisation, SEC-04) ;
  - supprimer les tokens de vérification et de réinitialisation expirés ;
  - supprimer les comptes non vérifiés de plus de 7 jours (avec leurs personnages, comme `CompteService.supprimerCompte`).
- Une seule instance sur Render : pas de ShedLock.

## Critères d'acceptation

- Après le job, plus aucun token expiré depuis plus de 7 jours ; un compte non vérifié de 8 jours a disparu, un compte de 6 jours est toujours là.

## Tests

Test d'intégration (Testcontainers) qui appelle directement la méthode du job.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/repository/RefreshTokenRepository.java`, `PasswordResetTokenRepository.java`, `EmailVerificationTokenRepository.java`, nouveau service de purge

## Décision (12/11/2026)

Les comptes non vérifiés ne sont **pas** supprimés (choix du propriétaire du projet) : la purge ne touche que les jetons. Le critère « un compte non vérifié de 8 jours a disparu » est donc abandonné ; `PurgeIT` vérifie au contraire qu'aucun compte n'est supprimé.

Fait : `PurgeService` (chaque jour à 4 h, heure de Paris, `PURGE_CRON`), requêtes `DELETE` directes dans `RefreshTokenRepository`, `EmailVerificationTokenRepository` et `PasswordResetTokenRepository`, `PlanificationConfig` (`@EnableScheduling`).

