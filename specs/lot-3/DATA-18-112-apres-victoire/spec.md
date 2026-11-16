# DATA-18 — 112 : le 248 aussi proposé après une victoire

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | S | `fix-DATA-18` | Fait |

Origine : relecture du 112 avec le porteur du projet (16/11/2026), après D-11

## Problème

Au 112, le livre propose deux suites après la victoire : « Si vous êtes vainqueur, vous pouvez explorer la grotte plus avant (33). Mais vous pouvez également quitter les lieux (248). » (VO : « If you win… 33. Or you may leave… 248. »)

Depuis D-11, le lien 112 → 248 est de type `evitement` : disponible seulement avant le premier assaut (ou après une fuite). Après une victoire, il était grisé (libellé « FUITE ») : seul le 33 restait.

Une condition ne peut pas valoir « avant le combat **ou** après une victoire » (les conditions d'un lien se cumulent), et rendre tout `evitement` disponible après une victoire ouvrirait aussi le 180 → 22 (« si vous souhaitez prendre la fuite »), ce que le livre ne propose pas.

## Fait

- `evitement` prend une valeur : sans valeur (180) = éviter le combat seulement ; `1` (112) = aussi après une victoire (`ConditionService`).
- `GameDataLoader` refuse toute autre valeur.
- Frontend : après une victoire, le lien affiche son numéro (248), plus « FUITE » (`ChapitrePage.combatGagne`).

## Critères d'acceptation

- 112 : avant le combat, départ vers 248 ; pendant, FUITE ; après une victoire, 33 et 248 au choix.
- 180 : après une victoire, seul le 62.

## Tests

`ConditionServiceTest` (evitement = 1 avant combat, pendant, après victoire), `GameDataLoaderTest` (valeurs acceptées et refusées), `LiensDeCombatTest` (112 → 248 = `evitement=1`), Vitest `chapitre.page.regles.spec.ts` (`combatGagne`).

## Fichiers concernés

`ConditionService.java`, `GameDataLoader.java`, `TypeCondition.java`, `chapitre.json`, `chapitre.page.ts`, `chapitre.page.html`
