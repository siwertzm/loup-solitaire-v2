# QUAL-19 — Commentaires et README alignés sur le code

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | XS | `fix-QUAL-19` | Fait |

Origine : audit Claude Code du 16/11/2026 (point 4)

## Problème

Le README backend disait que la mort consomme une Pièce Premium, alors que le code ne la retire jamais (règle MVP1 de `CLAUDE.md`) : quelqu'un pouvait « corriger » le code pour coller à la doc. Quelques commentaires décrivaient aussi un comportement disparu.

## Réalisé

- `backend/README.md` : Pièce Premium exigée mais non consommée en MVP1 (modèle de jeu, routes `revenir-apres-defaite` et `ressusciter`). À revoir avec REGLE-15.
- `ObjetService` : le retrait après consommation est fait par `PartieService.consommerObjet` / `CombatService.jouerObjet`, pas par un contrôleur.
- `CombatResponse` : `fuitePossible` couvre aussi le lien `EVITEMENT` (D-11).
- `EmailService` : commentaire orphelin remis sur `echapperHtml`.

## Fichiers concernés

`backend/README.md`, `ObjetService.java`, `CombatResponse.java`, `EmailService.java` — PR #97
