# DATA-13 — 260 : marqueur « sans arme » à la place de l'effet −4

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-DATA-13` | À faire |

Origine : Cahier des charges (DATA-13), dépend de REGLE-11

## Problème

`chapitre.json` au 260 : effet `habilite −4` sans condition.

## À faire

- Remplacer par le marqueur défini dans REGLE-11 ; JSON + migration dans le même commit.

## Critères d'acceptation

- Voir REGLE-11.

## Tests

`CoherenceLivreTest` vert.

## Fichiers concernés

`chapitre.json`, nouvelle migration
