# DATA-14 — 137 : 20 Pierres = un seul objet de Sac

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-DATA-14` | Fait |

Origine : Cahier des charges (DATA-14, décision D-04)

## Problème

`chapitre.json` au 137 : `pierre_precieuse` × 20 en catégorie BOURSE, qui compte dans les 50 pièces.

## Mise en œuvre (16/11/2026)

- Nouvel objet `pierres_tombeau`, nom « 20 Pierres Précieuses » (le nom est affiché, jamais la description), catégorie OBJET ; le 137 en donne 1 (automatique).
- Texte du 137 : « Elles prennent place dans votre bourse. » (PDF VF) remplacé par « Elles comptent pour un seul objet de votre Sac à Dos. » (VO).
- `pierre_precieuse` reste dans `objet.json` (plus utilisé par aucun chapitre) ; aucune condition ne le ciblait.

## À faire (spec d'origine)

- Nouvel objet `pierres_tombeau` (catégorie OBJET, quantité 1) au 137 ; `pierre_precieuse` n'est plus utilisé. Vérifier les conditions qui ciblaient `pierre_precieuse`.

## Critères d'acceptation

- Au 137 : 1 objet de Sac, la bourse ne change pas.

## Tests

Test données.

## Fichiers concernés

`objet.json`, `chapitre.json`
