# QUAL-12 — Afficher le bonus des Objets Spéciaux dans le Sac

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S/M | `feature-QUAL-12` | Fait |

Origine : retour bêta-testeur (16/11/2026)

## Problème

Dans le Sac (`inventaire-sheet`), les consommables (Potion de guérison, Laumspur, Essence d'Alether) affichent leur effet (`+4 ENDURANCE`) mais les Objets Spéciaux avec bonus n'affichent rien. Le joueur ne voit pas que le Casque donne +2 ENDURANCE et la Cotte de mailles +4.

Constat dans le code :
- `objetsEtRepasDetail` calcule `effetLabel` via `libelleEffet()`, seulement pour la catégorie OBJET.
- `objetsSpeciauxDetail` filtre l'inventaire sans chercher le catalogue : aucun `effetLabel`.
- Objets Spéciaux avec effet dans `objet.json` aujourd'hui : `casque` (endurance +2), `cotte_de_mailles` (endurance +4). L'effet est passif (`ObjetService` : appliqué à la récupération, retiré à la perte, touche aussi `enduranceMax`).

## À faire

- Dans `inventaire-sheet.component.ts`, calculer `effetLabel` pour les Objets Spéciaux qui ont un effet, en réutilisant `libelleEffet()`.
- Dans le template, afficher `<span class="sac-bonus">` à côté du nom, comme pour les consommables.
- Les Objets Spéciaux restent non cliquables (pas de popup « consommer » : l'effet est passif). La Carte garde son comportement.
- Page du personnage (`personnages/personnage.page.html`, carte ENDURANCE) : afficher le détail du bonus, `X + 4` pour la valeur et `X + 4 max` pour le plafond, quand le joueur a le Casque et/ou la Cotte de mailles. X = valeur sans le bonus des Objets Spéciaux. Sans ces objets, affichage inchangé.

## Décisions (16/11/2026)

- Libellé dans le Sac : identique aux potions (`+2 ENDURANCE`, clé `INVENTAIRE_SHEET.EFFET_LABEL`).
- Pas de distinction visuelle bonus passif / soin : juste afficher le bonus.
- Page du personnage : oui, détail `X + bonus` dans l'encart ENDURANCE (voir À faire).
- Pas d'autres objets concernés : seuls le Casque et la Cotte de mailles ont un effet.

## Décisions de mise en œuvre (16/11/2026)

- Fiche personnage : la valeur d'ENDURANCE reste le total, **sans détail** ; seul le maximum montre le bonus : « 24 + 4 Max » (`PERSONNAGE.DETAIL_MAX_BONUS`). Sans armure : « 28 Max » inchangé.
- Casque + Cotte : un seul total (« 22 + 6 Max »).
- Bonus calculé côté front (`bonusEnduranceObjetsSpeciaux`, `personnage.model.ts`) à partir de l'inventaire et du catalogue `GET /objets` ; aucun changement d'API.
- Sac : « +2 ENDURANCE » / « +4 ENDURANCE » sur les lignes du Casque et de la Cotte, comme les potions.

## Questions posées au départ

- `enduranceActuelle` et `enduranceMax` renvoyées par l'API incluent déjà le bonus (`ObjetService`). Si l'ENDURANCE actuelle tombe sous le bonus (ex. 3 avec la Cotte de mailles), que faut-il afficher : `-1 + 4`, `0 + 3`, ou seulement `3` ?
- Casque + Cotte de mailles ensemble : un seul total (`X + 6`) ou le détail (`X + 2 + 4`) ?
- Où calculer le bonus : côté front à partir de l'inventaire et du catalogue, ou nouveau champ dans `PersonnageResponse` (à modifier avec le modèle TS) ?

## Critères d'acceptation

- Le Casque affiche son bonus (+2 ENDURANCE) dans la section Objets Spéciaux du Sac, la Cotte de mailles +4.
- Un Objet Spécial sans effet n'affiche aucun bonus.
- Page du personnage : avec la Cotte de mailles, l'encart ENDURANCE affiche `X + 4` et `X + 4 max` ; sans objet à bonus, affichage inchangé.
- Aucun texte en dur : libellés dans `fr.json`.

## Tests

Tests Vitest : Casque → libellé `+2 ENDURANCE` dans le Sac, objet spécial sans effet → pas de libellé ; page du personnage avec / sans armure → détail du bonus présent / absent.

## Fichiers concernés

`frontend/src/app/features/shared/inventaire-sheet/`, `frontend/src/app/features/personnages/personnage.page.*`, `frontend/public/assets/i18n/fr.json`
