# REGLE-04 — Décrire DEFENSE et OBJET dans l'aide du combat

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `feature-REGLE-04` | Fait |

Origine : Cahier des charges (REGLE-04, décision D-01)

## Problème

Les actions DEFENSE et OBJET sont une règle maison (D-01) mais ne sont pas expliquées. Elles comptent comme des assauts pour les seuils des 231 et 339.

## Mise en œuvre (16/11/2026)

DEFENSE et OBJET étaient déjà décrites dans l'aide (`INTRO_COMBAT.PARAGRAPHE_DEFENDRE`, `PARAGRAPHE_OBJET`). Ajouté : `INTRO_COMBAT.PARAGRAPHE_ASSAUTS` (chaque action compte pour un assaut, seuils de victoire et de fuite), sans citer de chapitre. Tests `CombatServiceTest` : `defenseCompteCommeAssaut` (231), `objetCompteCommeAssaut` (339), `uneDefenseDebloqueLaFuiteDu339`. Code de jeu inchangé.

## À faire (spec d'origine)

- Compléter la page `regle/combat` (textes dans `fr.json`) : effets de DEFENSE, de OBJET, et le fait qu'elles comptent comme assaut.

## Critères d'acceptation

- La règle maison est affichée dans l'aide du combat.

## Tests

Test unitaire `defenseCompteCommeAssaut` (231, 339) côté backend.

## Fichiers concernés

`frontend/src/app/features/regle/combat/`, `fr.json`
