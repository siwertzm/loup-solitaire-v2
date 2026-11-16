# QUAL-03 — Convention de nommage unique, commentaires obsolètes

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | S | `fix-QUAL-03` | Fait |

Origine : Cahier des charges (QUAL-03)

## Problème

Routes en anglais (`/auth/register`), services en français ; coquille `viel_homme_fou` ; commentaires obsolètes (« combat non construit » dans `EffetChapitreService`).

Complété par l'audit Claude Code du 11/11/2026 :

- Noms anglais restants dans le code métier : `AuthController` (`getCurrentUser`, `updateProfil`, `changePassword`, `deleteAccount`), `ChangePasswordRequest` (`currentPassword`, `newPassword`), `DeleteAccountRequest`, `UpdateProfilRequest` ; côté front `auth.service.ts` (`setTokens`, `getPasswordResetEmail`, `getPasswordResetToken`) et des clés i18n (`*.BACK`, `*.TITLE_*`). À garder en anglais seulement si le framework l'impose (`CLAUDE.md`).
- Commentaires faux : `ConditionService` cite `PersonnageService.rafraichirTirageHasard` (n'existe pas) et dit que le tirage est « regénéré à chaque chargement du chapitre » (il est figé) ; `PersonnageService.avancerVersChapitre` parle d'un personnage « chargé par le contrôleur » (c'est `PartieService`).

## À faire

- Définir la convention avant la v1 publique ; corriger les commentaires obsolètes.

## Réalisé (16/11/2026)

- Convention écrite dans `CLAUDE.md` : URL et champs JSON existants figés en anglais (contrat avec les apps installées) ; noms internes, nouvelles routes et nouveaux champs en français.
- Renommés : `ChangementMotDePasseRequest`, `SuppressionCompteRequest`, `ModificationProfilRequest` ; méthodes de `AuthController` (`profil`, `modifierProfil`, `changerMotDePasse`, `supprimerCompte`) ; front `enregistrerJetons`, `emailReinitialisation`, `jetonReinitialisation`.
- Coquille `viel_homme_fou` → `vieil_homme_fou` ; commentaires de `ConditionService`, `EffetChapitreService`, `PersonnageService` corrigés.
- Restent en anglais (hors liste de l'audit, à renommer au fil de l'eau) : autres méthodes de `AuthController` et `auth.service.ts` (`register`, `login`, `forgotPassword`…), `ForgotPasswordRequest`, `ResetPasswordRequest`… ; clés i18n inchangées.

## Critères d'acceptation

—

## Tests

—

## Fichiers concernés

divers
