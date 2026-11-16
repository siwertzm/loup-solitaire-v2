# REGLE-18 — Guérison : pas de +1 ENDURANCE sur un chapitre de combat, d'effort violent ou de stress intense

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Moyenne | M | `feature-REGLE-18` | À faire |

Origine : demande du porteur du projet (16/11/2026), d'après la VF édition augmentée

## Problème

La Guérison rend +1 ENDURANCE à chaque chapitre sans combat. La **VF édition augmentée** est plus stricte : il faut « *aller d'un bout à l'autre d'un paragraphe sans combattre, fournir un effort violent ou subir un stress intense* ». L'app suit aujourd'hui la règle plus généreuse.

## Constat

- **Source retenue : VF édition augmentée** (porteur du projet, 16/11/2026) : « *…aller d'un bout à l'autre d'un paragraphe sans combattre, fournir un effort violent ou subir un stress intense…* ».
- Les autres versions ne citent que le combat : VO Project Aon tome 1 (`discplnz.htm` : « *in which you are not involved in combat* ») et la VF classique de `discipline.json` (« sans avoir à combattre un ennemi »). C'est un écart de version, choisi volontairement (écart à la décision D-02 « la VO tranche », à noter).
- **Code** : `PersonnageService` (fin de `avancerVersChapitre`) ajoute +1, plafonné à `enduranceMax`, si le chapitre n'est pas un combat, n'est pas une sous-section (> 350, REGLE-13) et que le personnage a Guérison.
- **Dégâts déjà repérables dans les données** : 16 chapitres sans combat ont un effet `endurance` négatif : 36, 76, 119, 144, 146, 158, 166, 188, 203, 236, 276, 304, 308, 313, 320, 343. Les Repas (7 chapitres, −3 si pas de Repas) sont aussi une perte possible.
- **« Effort » n'existe pas dans les données** : il faudra relire les 350 textes (VF + VO) pour repérer les chapitres d'effort (course, nage, escalade, fuite…) et les marquer dans `chapitre.json`.

## À faire (à préciser avec les réponses ci-dessous)

- Données : marquer les chapitres sans Guérison (ex. nouvel effet ou indicateur dans `chapitre.json`, rechargé par DATA-00) ; liste établie par relecture de chaque chapitre, présentée au porteur du projet pour validation avant de l'appliquer.
- Code : ne pas appliquer la Guérison sur ces chapitres (combat : déjà le cas).
- Textes : description de la Guérison (`discipline.json`) reprise de l'édition augmentée ; aide du jeu (`fr.json`, `en.json`) mise à jour.
- `CLAUDE.md` (« Guérison : +1 ENDURANCE par chapitre sans combat ») et `backend/README.md` mis à jour.

## À demander avant de commencer

- **Texte exact de l'édition augmentée** pour la Guérison (phrase complète) : à reprendre tel quel dans `discipline.json`.
- **« Effort violent » et « stress intense »** : la définition vient du texte, mais il faut décider chapitre par chapitre (course, fuite, nage, escalade, poursuite, peur, piège…). L'édition augmentée marque-t-elle elle-même ces chapitres, ou est-ce à l'appréciation du joueur ?
- **Dégâts** : bloquer la Guérison dès qu'un chapitre **a** un effet de perte d'ENDURANCE, ou seulement si la perte **est réellement subie** (ex. évitée grâce à une discipline ou un objet, ou tirage de hasard favorable au 188) ?
- **Repas** : un chapitre où il faut manger compte-t-il comme effort ? Seulement si le joueur n'a pas de Repas et perd 3 points ?
- **Méthode** : relecture complète des 350 chapitres par Claude avec une liste proposée (chapitre, raison, extrait du texte) que tu valides, ou liste fournie par toi ?
- **Affichage** : faut-il prévenir le joueur (ex. « Pas de Guérison : effort ») sur ces chapitres ?

## Critères d'acceptation

- Avec Guérison, un chapitre de dégâts ou d'effort (liste validée) ne rend aucun point ; un chapitre calme sans combat rend toujours +1.
- Règle de l'édition augmentée documentée (discipline, aide, `CLAUDE.md`, source notée).

## Tests

- Données : chaque chapitre de la liste validée porte le marqueur ; aucun chapitre de combat n'en a besoin.
- `PersonnageServiceTest` : Guérison sur un chapitre marqué → pas de +1 ; chapitre non marqué → +1.

## Fichiers concernés

`chapitre.json`, `discipline.json`, `PersonnageService.java`, éventuellement `GameDataLoader` / DTO si un nouveau champ, `fr.json`, `en.json`, `CLAUDE.md`, `backend/README.md`
