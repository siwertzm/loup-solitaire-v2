# OPS-06 — Sauvegardes, restauration et retour arrière

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 1 | Haute | M | `feature-OPS-06` | À faire |

Origine : Cahier des charges (OPS-06), statut « À faire »

## Problème

Aucune procédure de sauvegarde ni de restauration n'est définie ni testée pour la base PostgreSQL de Render. Une perte de données ferait perdre toutes les parties sans moyen de revenir en arrière.

## À faire

- Vérifier ce que le plan Render inclut : sauvegardes quotidiennes, restauration à un instant donné, expiration des bases gratuites.
- Écrire la procédure de restauration dans `backend/README.md` (étapes, durée mesurée).
- Tester une restauration sur un environnement de test, puis une fois par trimestre.
- Règle pour les migrations : rétrocompatibles (ajouter → migrer → supprimer) et `rollback` sur chaque changeset (déjà exigé par `CLAUDE.md`).

## Critères d'acceptation

- Une restauration a été faite sur un environnement de test, durée notée dans le README.
- Le plan Render et ses limites sont écrits dans le README.

## Tests

Procédure manuelle, chronométrée.

## Fichiers concernés

`backend/README.md`, réglages Render
