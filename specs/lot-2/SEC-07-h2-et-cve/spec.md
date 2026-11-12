# SEC-07 — H2 hors du jar de production ; contrôle des CVE en CI

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-SEC-07` | Fait |

Origine : Cahier des charges (SEC-07)

## Problème

`pom.xml` : H2 en scope `runtime`, donc embarqué dans le jar de production. Aucune vérification automatique des vulnérabilités des dépendances.

## À faire

- H2 en scope `test`.
- Activer Dependabot (Maven + npm) ou OWASP dependency-check dans la CI.

## Critères d'acceptation

- H2 absent du jar ; la CI signale une CVE de sévérité haute.

## Tests

`./mvnw verify` vert.

## Fichiers concernés

`backend/pom.xml`, `.github/`

## Résultat (12/11/2026)

- H2 en scope `test`, absent du jar.
- Scan initial : backend 4 CRITICAL (Tomcat 11.0.24) + 10 HIGH (Jackson 2.21.5 / 3.1.5), frontend 1 HIGH (`@angular/router` 21.2.23, SSR seulement). Corrigé par Spring Boot 4.1.2 (Tomcat 11.0.27, Jackson 3.1.7 / 2.21.7) et Angular 21.2.26 : 0 HIGH/CRITICAL.
- CI : `npm audit --omit=dev --audit-level=high` et Trivy sur le jar (`--ignore-unfixed`), bloquants. `.github/dependabot.yml` : PR hebdomadaires Maven, npm, GitHub Actions.

