# DATA-13 — 260 : marqueur « sans arme » à la place de l'effet −4

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-DATA-13` | Fait |

Origine : Cahier des charges (DATA-13), dépend de REGLE-11

## Problème

`chapitre.json` au 260 : effet `habilite −4` sans condition.

## À faire

- Remplacer par le marqueur défini dans REGLE-11 (`sans_arme`) ; JSON sans migration de données depuis DATA-00. Une migration de schéma reste nécessaire : `017-cond-sans-arme` recrée la contrainte CHECK de `cond.type` avec `SANS_ARME` (sans elle, PostgreSQL refuse le catalogue).

## Critères d'acceptation

- Voir REGLE-11.

## Tests

`CoherenceLivreTest` vert.

## Fichiers concernés

`chapitre.json`, nouvelle migration
