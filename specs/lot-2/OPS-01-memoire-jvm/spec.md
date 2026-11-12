# OPS-01 — Réglage mémoire de la JVM et sondes de santé

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-OPS-01` | Fait |

Origine : Cahier des charges (OPS-01)

## Problème

`Dockerfile` : `ENTRYPOINT ["java", "-jar", "/app/app.jar"]` sans réglage. La JVM ne prend que 25 % de la RAM du conteneur (128 Mo sur 512 Mo) : risque d'OutOfMemoryError ou de GC permanent.

## À faire

- `ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]`.
- `management.endpoint.health.probes.enabled=true`.

## Critères d'acceptation

- Test de charge à 50 utilisateurs simultanés sans OOM sur 512 Mo.

## Tests

Procédure manuelle (charge).

## Fichiers concernés

`backend/Dockerfile`, `application.properties`

## Résultat (12/11/2026)

Render : Docker, instance 1 CPU / 2 Go (et non 512 Mo comme supposé plus haut) : 75 % = 1,5 Go de tas. Image lancée en local avec `--cpus=1 -m 2g`, 50 joueurs simulés (`backend/scripts/charge.py`) :
- 3 min, ~1 s de réflexion : 165 req/s, p95 39 ms, mémoire max 455 Mo, aucune 5xx, pas d'OOM ;
- 1 min, ~0,2 s de réflexion : 525 req/s (CPU 100 %), p95 83 ms, mémoire max 451 Mo, pas d'OOM.

Sondes liveness/readiness publiques (`SecurityConfig`, testé dans `SecurityConfigTest`).

