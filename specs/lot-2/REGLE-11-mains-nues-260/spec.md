# REGLE-11 — 260 : combat à mains nues sans cumul des −4

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-REGLE-11` | Fait |

Origine : Cahier des charges (REGLE-11), à faire avec DATA-13

## Problème

Au 260, l'effet `habilite −4` s'ajoute au −4 déjà appliqué par `InventaireService.recalculerHabiliteArmes` quand le joueur n'a pas d'arme : −8 au total.

## À faire

- Condition « combat sans arme » qui force `habiliteBase − 4` pour ce combat, sans cumul (calcul dans `CombatService.habiliteEffective`).

## Critères d'acceptation

- Au 260, avec ou sans arme : HABILETÉ effective = base − 4.

## Tests

`CombatServiceTest`, deux cas.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/CombatService.java`, `EffetChapitreService.java`
