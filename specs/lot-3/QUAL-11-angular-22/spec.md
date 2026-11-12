# QUAL-11 — Migration vers Angular 22 et Vitest 5

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `feature-QUAL-11` | À faire |

Origine : PR Dependabot du 12/11/2026 (#42 à #45), fermées volontairement

## Problème

Angular 22 et Vitest 5 sont sortis. Dependabot les proposait paquet par paquet (`@angular/common`, `@angular/build`, `@angular/cli`, `vitest`) : pris séparément, `@angular/common` et `@angular/build` cassaient les tests frontend, et `@angular/cli` 22 avec une app en 21 aurait généré du code pour la mauvaise version. Vitest 5 passait la CI, mais `@angular/build` 21 n'accepte officiellement que `vitest ^4` (l'installation avec `--legacy-peer-deps` masque l'écart).

Angular 21 reste maintenu un temps ; rester dessus n'est pas un risque immédiat (les failles sont contrôlées en CI, SEC-07).

## À faire

- `npx ng update @angular/core@22 @angular/cli@22` : met à jour tous les paquets Angular ensemble et applique les migrations automatiques du code.
- Vérifier la compatibilité d'Ionic 9, Capacitor 8, ngx-translate et `@sentry/angular` avec Angular 22 ; les mettre à jour si besoin.
- Vitest 5 seulement si `@angular/build` 22 l'accepte (`npm view @angular/build@22 peerDependencies`).
- Lire le guide de migration (https://angular.dev/update-guide) pour les changements non automatisés.

## Critères d'acceptation

- `npm test`, `npm run build` et la CI verts.
- App testée sur téléphone (Android et iOS via TestFlight) : connexion, création de personnage, chapitre, combat, inventaire, profil.
- `npm audit --omit=dev --audit-level=high` sans alerte.

## Tests

Tests existants (QUAL-06, QUAL-10) + parcours manuel ci-dessus.

## Fichiers concernés

`frontend/package.json`, `frontend/package-lock.json`, code modifié par les migrations de `ng update`
