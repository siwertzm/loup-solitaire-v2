# SEC-08 — 409 sur violation d'unicité, 500 générique journalisé

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-SEC-08` | Fait |

Origine : Cahier des charges (SEC-08)

## Problème

`GlobalExceptionHandler` ne gère ni `DataIntegrityViolationException` (deux inscriptions simultanées avec le même email) ni `IllegalStateException` : le joueur reçoit une 500 brute.

## À faire

- Gestionnaire 409 pour `DataIntegrityViolationException`.
- Repli générique 500 qui journalise l'erreur avec un identifiant, sans renvoyer le message interne.

## Critères d'acceptation

- Deux inscriptions parallèles avec le même email → une 201 et une 409.

## Tests

`GlobalExceptionHandlerTest` ou test d'intégration.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/exception/GlobalExceptionHandler.java`
