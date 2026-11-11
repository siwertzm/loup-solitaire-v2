# OPS-07 — Moins de requêtes SQL par action

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `fix-OPS-07` | À faire |

Origine : Cahier des charges (OPS-07)

## Problème

`listerInventaire` est appelé plusieurs fois par action ; N+1 sur la liste des personnages. Sans effet sous 1 000 joueurs ; le pool de 10 connexions devient la limite vers 10 000 joueurs actifs.

## À faire

- Mémoriser l'inventaire dans un contexte de requête (comme `ContexteConditions`) ; `@BatchSize` ; pool de 20 et instance de 1 Go au-delà de 1 000 joueurs simultanés.

## Critères d'acceptation

- Au plus 15 requêtes SQL pour `POST /chapitre/{id}` (statistiques Hibernate).

## Tests

Test qui compte les requêtes.

## Fichiers concernés

services de jeu, mappers
