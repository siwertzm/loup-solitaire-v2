# QUAL-21 — Tables de combat de la page des règles vérifiées contre le serveur

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | S | `fix-QUAL-21` | Fait |

Origine : second audit Claude Code du 17/11/2026 (point 2)

## Problème

La page des règles affiche les tables de combat (`frontend/src/app/features/regle/combat/table-combat.data.ts`). Le serveur a sa propre copie, utilisée en vrai combat (`TableCombatService`).

Les deux copies sont identiques au 17/11/2026 (le serveur stocke les dégâts en négatif, `-999` = coup fatal). Mais les tables sont maison et se rééquilibrent (D-12) : si le prochain changement ne touche que `TableCombatService`, aucun test n'échoue et la page des règles montre des chiffres qui ne servent plus.

## À faire

Un test backend qui lit `table-combat.data.ts` (le dépôt est un monorepo : le fichier est présent en CI) et le compare à `TableCombatService` : dégâts infligés et subis (signe inversé, `FATAL` ↔ `-999`), réduction et bonus de DEFENSE. Environ 30 lignes, pas de nouvelle route.

## Critères d'acceptation

Changer une seule valeur d'un côté fait échouer `./mvnw test`.

## Fichiers concernés

Nouveau test sous `backend/src/test/.../service/` ; `table-combat.data.ts` et `TableCombatService.java` en lecture

## Réalisation (17/11/2026)

`TablesCombatPageReglesTest` (JUnit simple, sans Spring) extrait les quatre tableaux du `.ts` et les compare case par case aux méthodes publiques de `TableCombatService`, dimensions comprises. Vérifié : une valeur changée dans le `.ts` (DEGATS_INFLIGES) puis dans le `.java` (BONUS_HABILITE) fait échouer le test, avec la table, la colonne et le chiffre dans le message.
