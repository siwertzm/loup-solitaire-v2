# SEC-07 — H2 hors du jar de production ; contrôle des CVE en CI

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-SEC-07` | À faire |

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
