# RGPD-03 — Durées de conservation ; emails masqués dans les logs

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-RGPD-03` | Fait |

Origine : Cahier des charges (RGPD-03)

## Problème

`EmailService` journalise l'adresse email complète dans les erreurs (ex. « Echec de l'envoi … a marius@example.com »). Comptes non vérifiés gardés indéfiniment : décision du 12/11/2026 (OPS-05), ils ne sont pas supprimés ; à écrire dans la politique de confidentialité.

## À faire

- Masquer l'email dans les logs (`j***@domaine`), via un helper dans `util/Emails`.
- Durées de conservation écrites dans la politique de confidentialité.

## Critères d'acceptation

- Aucun email complet dans les logs.

## Tests

Test unitaire du masquage.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/EmailService.java`, `backend/src/main/java/com/loupsolitaire/backend/util/Emails.java`

## Fait (12/11/2026)

- `Emails.masquer` (adresse) et `Emails.masquerDansTexte` (toutes les adresses d'un message d'erreur) ; utilisés dans les 6 logs de `EmailService`.
- `GlobalExceptionHandler` : une violation d'unicité est journalisée avec le seul nom de la contrainte (le message de PostgreSQL contenait l'email en double, depuis SEC-08).
- `LogsSansEmailTest` : contrôle du code source, échoue si un `log.xxx(...)` reçoit une variable ou un getter d'email non masqué.
- Politique de confidentialité : comptes non confirmés gardés, adresse en attente (SEC-06), purge automatique des sessions et liens expirés (OPS-05), adresses masquées dans les journaux.

