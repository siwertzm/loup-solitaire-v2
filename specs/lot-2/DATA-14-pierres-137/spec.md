# DATA-14 — 137 : 20 Pierres = un seul objet de Sac

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-DATA-14` | À faire |

Origine : Cahier des charges (DATA-14, décision D-04)

## Problème

`chapitre.json` au 137 : `pierre_precieuse` × 20 en catégorie BOURSE, qui compte dans les 50 pièces.

## À faire

- Nouvel objet `pierres_tombeau` (catégorie OBJET, quantité 1) au 137 ; `pierre_precieuse` n'est plus utilisé. Vérifier les conditions qui ciblaient `pierre_precieuse`.

## Critères d'acceptation

- Au 137 : 1 objet de Sac, la bourse ne change pas.

## Tests

Test données.

## Fichiers concernés

`objet.json`, `chapitre.json`
