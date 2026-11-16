# REGLE-12 — Le Sac à Dos devient un objet qu'on peut perdre

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | M/L | `feature-REGLE-12` | À faire |

Origine : Cahier des charges (REGLE-12, décision D-06), revu par le porteur du projet le 16/11/2026

## Problème

Le Sac à Dos n'existe pas dans le modèle : on peut toujours porter des objets et des repas. Dans le livre, le Sac est perdu à plusieurs endroits, et sans Sac on ne peut plus porter d'objets de Sac (objets et repas) jusqu'à en retrouver un.

## Décision (16/11/2026)

Le Sac à Dos est un **objet** de catégorie **Objets Spéciaux** (`sac_a_dos`), et non un booléen `possedeSac` sur `Personnage` comme prévu par D-06. On peut le perdre et le retrouver comme n'importe quel objet.

## Constat dans le livre (VO Project Aon) et dans `chapitre.json`

**Perte du Sac** :

| § | VO | `chapitre.json` aujourd'hui |
| --- | --- | --- |
| 162 | « *They take your Backpack and Weapons* » (or gardé) | **aucun effet** : armes et objets ne sont pas retirés |
| 174 | « *you lose your Backpack and Weapons* » | `vol` 10 (armes + objets) |
| 188 | tirage 0-6 : « *the Kraan has ripped away your Backpack* » | `vol` 8 sur tirage 0-6 (objets) |
| 258 | « *You have lost your Backpack and Weapons* » | `vol` 10 |
| 294 | « *You have lost your Weapon(s) and Backpack* » | `vol` 10 |

Cas à part : au **205** (DATA-06), la VO dit d'effacer « *all Weapons and Backpack Items* » : les objets, **pas** le Sac lui-même.

**Sac trouvé** :
- **20** : « *you find a Backpack, food (enough for two Meals), and a Dagger* ».
- **62** : « *two Backpacks containing enough food for 3 Meals* ».

Liés : 277 et 338 (le Sac est retrouvé intact sur place, pas de perte) ; 144 (REGLE-09 : « *If you no longer have a Backpack* [...] *you lose a Weapon* »).

## À faire

- `objet.json` : nouvel objet `sac_a_dos`, catégorie Objets Spéciaux.
- Équipement de départ : le personnage commence avec le Sac (`PersonnageService`, matériel fixe avec Hache, Repas, Carte).
- Sans Sac : ramasser un objet OBJET ou REPAS → 400 (règle métier, message clair). Perdre le Sac fait perdre tous les objets OBJET et REPAS.
- Perte du Sac aux 162, 174, 188 (tirage 0-6), 258, 294 (`chapitre.json`, rechargé par DATA-00). Le 162 doit aussi retirer les armes (manque actuel).
- `sac_a_dos` ramassable aux 20 et 62.
- La règle de repas (`REPAS` d'un chapitre) reste inchangée : sans Sac, pas de Repas, donc −3 ENDURANCE sauf Chasse.
- Frontend : afficher le Sac dans les Objets Spéciaux ; sans Sac, la section Objets & Repas l'indique (textes dans `fr.json` / `en.json`).
- `objet.json` / `chapitre.json` seulement pour les données ; une migration Liquibase seulement si le modèle change (ex. contrainte CHECK, voir mémo enums).
- Mettre à jour `CLAUDE.md` (règles d'inventaire) et `backend/README.md` (modèle de jeu).

## À demander avant de commencer

- **205** : la VO fait perdre les objets mais pas le Sac. On garde le Sac au 205 (fidèle à la VO), ou on le retire aussi ?
- **Comment coder la perte du Sac** dans les effets : nouvelle valeur de `vol` (ex. Sac + armes, Sac seul), ou perte de l'objet `sac_a_dos` via les valeurs 8 / 10 actuelles ? Les valeurs 8 et 10 servent aussi au 205, où le Sac est gardé. À trancher au récap.
- **Retrait volontaire** : le joueur peut-il jeter son Sac depuis l'inventaire (swipe) ? Si oui, il perd tous ses objets et repas : faut-il un avertissement, ou interdire ce retrait ?
- **Au 62** (deux Sacs) : un seul ramassable (le second ne sert à rien), d'accord ?
- **Personnages existants** : leur donner le Sac au déploiement (y compris ceux qui ont déjà passé un chapitre de perte), ou seulement à ceux qui n'ont pas perdu leur Sac d'après leur historique ?
- **Ordre avec REGLE-09** (vol du 144 « Sac d'abord ») et **DATA-14** (Pierres du 137 = objet de Sac) : ils dépendent de ce modèle, à faire juste après.
- **QUAL-12** (bonus des Objets Spéciaux) : le Sac n'a pas de bonus, rien à afficher ; à confirmer.

## Critères d'acceptation

- Un nouveau personnage a le Sac dans ses Objets Spéciaux.
- Après le 174 : plus de Sac, ni armes ni objets ; ramasser un Repas → 400.
- Au 20, prendre le Sac : ramasser un Repas redevient possible.
- Au 162 : Sac et armes retirés, or gardé.
- Au 188 : Sac perdu seulement sur un tirage 0-6.

## Tests

- Données (`data/`) : perte du Sac aux 162, 174, 188, 258, 294 ; `sac_a_dos` aux 20 et 62.
- `InventaireServiceTest` : ramassage OBJET/REPAS sans Sac → 400 ; avec Sac → OK.
- `EffetChapitreServiceTest` : la perte du Sac retire les objets et repas.
- `PersonnageServiceTest` : le Sac est dans l'équipement de départ.
- Vitest si le frontend gagne de la logique (état « sans Sac »).

## Fichiers concernés

`objet.json`, `chapitre.json`, `PersonnageService.java`, `InventaireService.java`, `EffetChapitreService.java`, éventuellement une migration, `inventaire-sheet` (frontend), `fr.json`, `en.json`, `CLAUDE.md`, `backend/README.md`
