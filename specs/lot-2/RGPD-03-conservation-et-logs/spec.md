# RGPD-03 — Durées de conservation ; emails masqués dans les logs

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-RGPD-03` | À faire |

Origine : Cahier des charges (RGPD-03)

## Problème

`EmailService` journalise l'adresse email complète dans les erreurs (ex. « Echec de l'envoi … a marius@example.com »). Comptes non vérifiés gardés indéfiniment (traité par OPS-05).

## À faire

- Masquer l'email dans les logs (`j***@domaine`), via un helper dans `util/Emails`.
- Durées de conservation écrites dans la politique de confidentialité.

## Critères d'acceptation

- Aucun email complet dans les logs.

## Tests

Test unitaire du masquage.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/EmailService.java`, `backend/src/main/java/com/loupsolitaire/backend/util/Emails.java`
