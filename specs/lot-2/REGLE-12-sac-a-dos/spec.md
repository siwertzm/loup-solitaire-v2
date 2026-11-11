# REGLE-12 — Modéliser le Sac à Dos

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | M | `feature-REGLE-12` | À faire |

Origine : Cahier des charges (REGLE-12, décision D-06)

## Problème

Le Sac à Dos n'existe pas dans le modèle. Aux 174, 188 (tirage 0-6), 258 et 294, le livre fait perdre le Sac lui-même, mais l'app laisse ramasser des objets de Sac juste après.

## À faire

- Booléen `possedeSac` sur `Personnage` (migration, `true` par défaut).
- Perte du Sac aux 174, 188, 258, 294 ; objet `sac_a_dos` ramassable aux 20 et 62.
- Ramasser un objet OBJET/REPAS sans Sac → 400.
- Afficher l'état du Sac dans l'inventaire (frontend).

## Critères d'acceptation

- Après le 174, ramasser un Repas → 400 ; après avoir pris le Sac au 20, c'est de nouveau possible.

## Tests

`PersonnageServiceTest`, `InventaireServiceTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/model/Personnage.java`, `InventaireService.java`, `EffetChapitreService.java`, données, migration, frontend
