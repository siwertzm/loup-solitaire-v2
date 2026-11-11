# DATA-07 — 181 : suppression du vol du Sac

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-DATA-07` | À faire |

Origine : Cahier des charges (DATA-07, décision D-03), avec DATA-06

## Problème

`chapitre.json` au 181 : `vol` valeur 8, toujours appliqué, alors qu'on peut y arriver par le 22 sans avoir rien perdu (errata VO).

## À faire

- Supprimer l'effet `vol` du 181 ; JSON + migration (ou DATA-00). À livrer avec DATA-06.

## Critères d'acceptation

- Arrivée au 181 par le 22 : inventaire intact.

## Tests

Test données.

## Fichiers concernés

`chapitre.json`
