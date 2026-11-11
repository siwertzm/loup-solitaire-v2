# REGLE-06 — Interdire de jeter un objet pendant un combat

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-REGLE-06` | À faire |

Origine : Cahier des charges (REGLE-06)

## Problème

`PartieService.retirerObjet` (`DELETE /objets/{objetId}`) ne vérifie pas le combat. Le livre l'interdit.

## À faire

- Même garde que `consommerObjet` (combat `EN_COURS` sur le chapitre → `IllegalArgumentException`). Factoriser la garde dans une méthode privée de `PartieService`.

## Critères d'acceptation

- DELETE pendant un combat EN_COURS → 400.

## Tests

`PartieServiceTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/PartieService.java`
