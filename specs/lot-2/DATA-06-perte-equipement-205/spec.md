# DATA-06 — 205 : perte des armes et du Sac (vol 10)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-DATA-06` | À faire |

Origine : Cahier des charges (DATA-06, décision D-03), avec DATA-07

## Problème

`chapitre.json` au 205 : aucun effet. Le livre (VO) : le chef ramasse tout l'équipement.

## À faire

- Ajouter au 205 `{ type: vol, nom: perte, valeur: 10 }` ; JSON + migration (ou DATA-00).

## Critères d'acceptation

- Arrivée au 205 : armes et objets du Sac retirés.

## Tests

`CoherenceLivreTest` / test données.

## Fichiers concernés

`chapitre.json`
