# REGLE-04 — Décrire DEFENSE et OBJET dans l'aide du combat

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `feature-REGLE-04` | À faire |

Origine : Cahier des charges (REGLE-04, décision D-01)

## Problème

Les actions DEFENSE et OBJET sont une règle maison (D-01) mais ne sont pas expliquées. Elles comptent comme des assauts pour les seuils des 231 et 339.

## À faire

- Compléter la page `regle/combat` (textes dans `fr.json`) : effets de DEFENSE, de OBJET, et le fait qu'elles comptent comme assaut.

## Critères d'acceptation

- La règle maison est affichée dans l'aide du combat.

## Tests

Test unitaire `defenseCompteCommeAssaut` (231, 339) côté backend.

## Fichiers concernés

`frontend/src/app/features/regle/combat/`, `fr.json`
