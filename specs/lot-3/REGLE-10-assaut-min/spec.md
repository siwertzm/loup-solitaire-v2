# REGLE-10 — Condition ASSAUT_MIN (effet à partir du 2e assaut)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `feature-REGLE-10` | À faire |

Origine : Cahier des charges (REGLE-10)

## Problème

`CombatService.bonusHabiliteAssautMax` ne gère qu'un effet dont l'unique condition est ASSAUT_MAX ; « à partir du 2e assaut » (283) n'est pas exprimable.

## À faire

- Nouveau type `ASSAUT_MIN`, combinable avec DISCIPLINE ; mettre à jour la contrainte CHECK `cond_type_check` (migration).

## Critères d'acceptation

- Au 283 : +2 au 1er assaut, −2 ensuite sans Bouclier Psychique, 0 ensuite avec.

## Tests

`CombatServiceTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/CombatService.java`, `model/enums/TypeCondition.java`, migration
