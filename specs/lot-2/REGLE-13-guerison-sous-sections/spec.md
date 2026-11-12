# REGLE-13 — Pas de Guérison sur les sous-sections 2101 et 2102

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-REGLE-13` | Fait |

Origine : Cahier des charges (REGLE-13)

## Problème

`PersonnageService.appliquerGuerison` s'applique aussi aux sous-sections 2101 et 2102 : jusqu'à +2 ENDURANCE en trop sur la section 21.

## À faire

- Ne pas appliquer la Guérison aux identifiants > 350 (constante nommée).

## Critères d'acceptation

- Parcours 21 → 2101 → 2102 : +1 au total.

## Tests

`PersonnageServiceTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/PersonnageService.java`
