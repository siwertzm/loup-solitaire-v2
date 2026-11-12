# QUAL-02 — Préfixe /api/v1, OpenAPI, version minimale de l'app

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | M | `feature-QUAL-02` | Fait |

Origine : Cahier des charges (QUAL-02)

## Problème

Pas de version d'API ni de contrat. Les apps installées (TestFlight, Play) ne se mettent pas à jour de force : un changement de contrat casse les anciennes versions.

## À faire

- Endpoint public `/version` avec la version minimale de l'app ; l'app affiche « mettez à jour » si elle est trop ancienne. **C'est la partie la plus utile, à faire en premier.**
- Préfixe `/api/v1` (garder l'ancien chemin le temps que les apps se mettent à jour).
- OpenAPI généré.

## Critères d'acceptation

- Une app plus ancienne que la version minimale affiche l'écran de mise à jour.

## Tests

Test de contrôleur.

## Fichiers concernés

`SecurityConfig.java`, contrôleurs, `environment.*.ts`
