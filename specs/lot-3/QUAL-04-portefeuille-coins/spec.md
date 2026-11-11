# QUAL-04 — Portefeuille de coins par utilisateur

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Haute (MVP2) | L | `feature-QUAL-04` | À faire |

Origine : Cahier des charges (QUAL-04)

## Problème

Le coin est un objet à 999 exemplaires par personnage. Impossible de vendre des coins sans refonte.

## À faire

- Portefeuille par utilisateur (journal des mouvements avec clé d'idempotence), validation serveur des reçus Google Play et App Store, coin retiré de l'inventaire.

## Critères d'acceptation

- Un achat rejoué deux fois ne crédite qu'une fois.

## Tests

Tests d'intégration.

## Fichiers concernés

nouveau module
