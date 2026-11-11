# OPS-08 — `backend/mvnw` sans droit d'exécution dans git

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 1 | Basse | S | `fix-OPS-08` | À faire |

Origine : Audit Claude Code du 11/11/2026 (nouveau)

## Problème

`backend/mvnw` est enregistré en mode `100644`. Sur un clone neuf sous Linux, macOS ou WSL, `./mvnw` (commande donnée dans `CLAUDE.md`) échoue avec « Permission denied ». La CI n'est pas touchée car elle appelle `mvn`.

## À faire

- `git update-index --chmod=+x backend/mvnw` (déjà fait en local avec `chmod +x`, reste à committer).

## Critères d'acceptation

- `git ls-files -s backend/mvnw` affiche `100755`.

## Tests

Aucun.

## Fichiers concernés

`backend/mvnw`
