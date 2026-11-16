# REGLE-10 — Condition ASSAUT_MIN (effet à partir du 2e assaut)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `feature-REGLE-10` | Fait |

Origine : Cahier des charges (REGLE-10)

## Problème

`CombatService.bonusHabiliteAssautMax` ne gère qu'un effet dont l'unique condition est ASSAUT_MAX ; « à partir du 2e assaut » (283) n'est pas exprimable.

## À faire

- Nouveau type `ASSAUT_MIN`, combinable avec DISCIPLINE ; mettre à jour la contrainte CHECK `cond_type_check` (migration).

## Critères d'acceptation

- Au 283 : +2 au 1er assaut, −2 ensuite sans Bouclier Psychique, 0 ensuite avec.

## Réalisé (16/11/2026)

- `ASSAUT_MIN` (migration 019) ; `CombatService.bonusHabiliteAssaut` gère `ASSAUT_MAX` / `ASSAUT_MIN` avec protections DISCIPLINE/OBJET, assaut par assaut.
- Demande du porteur du projet : le bonus/malus de l'assaut à venir est exposé (`CombatResponse.bonusHabiliteAssaut`) et affiché sur la carte du joueur (HAB + picto), et sur l'écran du chapitre (« À partir de l'assaut 2 », badge « ÉVITÉ »).
- Un joueur déjà au 283 au déploiement garde le −2 dans son HABILETÉ temporaire jusqu'au changement de chapitre (−4 à partir du 2e assaut) : cas rare, accepté.

## Tests

`CombatServiceTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/CombatService.java`, `model/enums/TypeCondition.java`, migration
