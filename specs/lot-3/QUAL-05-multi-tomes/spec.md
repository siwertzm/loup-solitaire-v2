# QUAL-05 — Modèle multi-tomes

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Moyenne (MVP2) | L | `feature-QUAL-05` | À faire |

Origine : Cahier des charges (QUAL-05)

## Problème

L'identifiant de chapitre est un entier global, les sous-sections sont codées 2101/2102, objets et ennemis n'ont pas de tome, et les types sont figés par des contraintes CHECK. Collision dès le tome 2.

## À faire

- Colonne `livre` (clé composée livre + numéro), champ « sous-section », `personnage.livre`, import d'un personnage d'un tome à l'autre.

## Critères d'acceptation

- Un tome 2 fictif de 3 sections se charge à côté du tome 1.

## Tests

Tests d'intégration.

## Fichiers concernés

modèle, données, migrations
