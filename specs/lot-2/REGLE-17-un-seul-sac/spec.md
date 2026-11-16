# REGLE-17 — Un seul Sac à Dos à la fois

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-REGLE-17` | Fait |

Origine : test du porteur du projet (16/11/2026), suite de REGLE-12

## Problème

Depuis REGLE-12, un Sac à Dos est ramassable aux 20 et 62. Rien n'empêchait d'en prendre un second alors que le joueur en avait déjà un : un second Sac ne sert à rien.

## Fait

- Backend : `PersonnageService.ramasserObjetDuChapitre` refuse `sac_a_dos` si le joueur en possède déjà un (400 « Vous avez deja un Sac a Dos »). Un Sac perdu peut être repris.
- Frontend `chapitre-objets` : bouton grisé « DÉJÀ » (`CHAPITRE_OBJETS.BADGE_DEJA_SAC`, en « ALREADY ») à la place de PRENDRE.
- Frontend `chapitre.page` : l'alerte « objets non ramassés » ne compte pas ce Sac.
- Effet de bord de REGLE-12 corrigé au passage : l'écran de révélation de l'équipement de départ (`inventaire-depart.page.ts`) présentait parfois le Sac à Dos comme l'objet tiré au hasard (il cherche « le seul objet hors équipement fixe » et ne connaissait pas le Sac). Le Sac est ajouté à l'équipement fixe de cet écran, visible d'entrée comme la Carte.

## Critères d'acceptation

- Avec un Sac : la ligne Sac à Dos affiche « DÉJÀ », l'API répond 400, quitter le chapitre ne déclenche pas d'alerte pour ce Sac.
- Sans Sac : PRENDRE fonctionne.

## Tests

`PersonnageServiceTest` (second Sac refusé, Sac repris si perdu) ; Vitest `chapitre-objets.component.spec.ts`, `chapitre.page.spec.ts` et `inventaire-depart.page.spec.ts`.

## Fichiers concernés

`PersonnageService.java`, `chapitre-objets.component.{ts,html}`, `chapitre.page.ts`, `inventaire-depart.page.ts`, `fr.json`, `en.json`
