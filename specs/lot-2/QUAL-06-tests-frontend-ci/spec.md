# QUAL-06 — Aucun test frontend, et la CI ne lance pas de tests frontend

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `feature-QUAL-06` | À faire |

Origine : Audit Claude Code du 11/11/2026 (nouveau)

## Problème

`frontend/src` ne contient aucun fichier `.spec.ts`, et les workflows ne lancent que `npm run build`. Le code le plus risqué de l'app (intercepteur de session, SEC-09) n'a aucun test : une régression ne serait vue que par les testeurs.

## À faire

- Premier test : `auth.interceptor.spec.ts` (voir SEC-09).
- Ajouter `npm test -- --watch=false` dans les workflows qui construisent le frontend.
- Règle : toute nouvelle logique frontend non triviale (service, intercepteur, guard) vient avec un test.

## Critères d'acceptation

- La CI échoue si un test frontend échoue.

## Tests

Le test de SEC-09.

## Fichiers concernés

`frontend/src/app/core/interceptors/`, `.github/workflows/*.yml`
