# REGLE-14 — Choix « Sinon » exclusifs (valeur −1 sur les liens)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-REGLE-14` | Fait |

Origine : Cahier des charges (REGLE-14)

## Problème

Sur un lien, la valeur −1 (« si vous ne possédez pas ») est ignorée par `ConditionService`. Exemple : avec la Pierre de Vordak, le joueur peut quand même choisir la mort du 292 depuis le 9. Concerne 9, 52, 105, 128, 162, 173, 242, 272, 303.

## À faire

- Interpréter `valeur = -1` comme « ne possède pas » ; ajouter ces conditions dans `chapitre.json` (sans migration depuis DATA-00 : le catalogue se recharge au démarrage).

## Critères d'acceptation

- Au 9 avec la Pierre de Vordak, le lien vers 292 est indisponible.

## Tests

`ConditionServiceTest`, `CoherenceLivreTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/ConditionService.java`, `chapitre.json`, migration
