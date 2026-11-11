# QUAL-03 — Convention de nommage unique, commentaires obsolètes

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | S | `fix-QUAL-03` | À faire |

Origine : Cahier des charges (QUAL-03)

## Problème

Routes en anglais (`/auth/register`), services en français ; coquille `viel_homme_fou` ; commentaires obsolètes (« combat non construit » dans `EffetChapitreService`).

Complété par l'audit Claude Code du 11/11/2026 :

- Noms anglais restants dans le code métier : `AuthController` (`getCurrentUser`, `updateProfil`, `changePassword`, `deleteAccount`), `ChangePasswordRequest` (`currentPassword`, `newPassword`), `DeleteAccountRequest`, `UpdateProfilRequest` ; côté front `auth.service.ts` (`setTokens`, `getPasswordResetEmail`, `getPasswordResetToken`) et des clés i18n (`*.BACK`, `*.TITLE_*`). À garder en anglais seulement si le framework l'impose (`CLAUDE.md`).
- Commentaires faux : `ConditionService` cite `PersonnageService.rafraichirTirageHasard` (n'existe pas) et dit que le tirage est « regénéré à chaque chargement du chapitre » (il est figé) ; `PersonnageService.avancerVersChapitre` parle d'un personnage « chargé par le contrôleur » (c'est `PartieService`).

## À faire

- Définir la convention avant la v1 publique ; corriger les commentaires obsolètes.

## Critères d'acceptation

—

## Tests

—

## Fichiers concernés

divers
