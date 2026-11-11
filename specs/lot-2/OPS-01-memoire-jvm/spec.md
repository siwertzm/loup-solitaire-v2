# OPS-01 — Réglage mémoire de la JVM et sondes de santé

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-OPS-01` | À faire |

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
