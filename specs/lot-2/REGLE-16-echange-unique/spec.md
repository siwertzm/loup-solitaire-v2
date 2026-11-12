# REGLE-16 — L'échange du 307 peut être répété en appelant l'API

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-REGLE-16` | Fait |

Origine : Audit Claude Code du 11/11/2026 (nouveau)

## Problème

Au 307, l'ermite échange son Marteau de Guerre contre une arme du joueur. Le front cache le bouton dès que le joueur possède l'objet proposé (`chapitre-effets.component.ts`, `echangeDejaFait`). Le serveur, lui, ne vérifie rien de tel (`PersonnageService.echangerObjet`) : un appel direct à `POST /personnages/{id}/objets/marteau/echanger-contre/hache` fonctionne une deuxième fois et donne deux Marteaux.

La règle « le serveur fait autorité » (`CLAUDE.md`) n'est donc pas tenue sur ce point.

## À faire

- Dans `PersonnageService.echangerObjet`, refuser (`IllegalArgumentException`, 400) si le personnage possède déjà `objetAAjouter`. C'est la même règle que le front, déplacée côté serveur.

## Critères d'acceptation

- Premier échange au 307 → accepté.
- Deuxième échange alors que le Marteau est encore dans l'inventaire → 400.

## Tests

- `PersonnageServiceTest.echangeRefuseSiObjetDejaPossede`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/PersonnageService.java`, `backend/src/test/java/com/loupsolitaire/backend/service/PersonnageServiceTest.java`

## Ajout (12/11/2026) : alerte avant de quitter le chapitre

L'échange étant un effet (onglet Effets), le joueur pouvait changer de chapitre sans l'avoir vu. L'alerte qui existait pour les objets non ramassés couvre aussi un échange encore possible : effet `ECHANGE` sur le chapitre, objet proposé pas encore possédé, et au moins un objet de la même catégorie à donner (`ChapitrePage.echangesNonFaits`). Popup « AVANT DE PARTIR » avec « VOIR L'ÉCHANGE » (onglet Effets). Tests : `chapitre.page.spec.ts`.

