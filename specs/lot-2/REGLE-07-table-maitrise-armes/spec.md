# REGLE-07 — Table 0-9 de la Maîtrise des Armes

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-REGLE-07` | Fait |

Origine : Cahier des charges (REGLE-07)

## Problème

`PersonnageService.tirerArmeMaitrisee` tire uniformément parmi les armes du catalogue. Le livre utilise une table 0-9 où l'Épée sort sur 5 et 7 (20 % au lieu de 11 %).

## À faire

- Table fixe : 0 poignard, 1 lance, 2 masse, 3 sabre, 4 marteau, 5 épée, 6 hache, 7 épée, 8 bâton, 9 glaive (comme `TABLE_OBJET_DEPART`).

## Critères d'acceptation

- Chaque tirage 0-9 donne l'arme de la table.

## Tests

Test paramétré sur les 10 tirages (`Random` bouchonné).

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/PersonnageService.java`
