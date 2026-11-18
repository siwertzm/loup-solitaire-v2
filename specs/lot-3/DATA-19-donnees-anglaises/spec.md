# DATA-19 — Jeu de données anglais du livre

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M (par fichier : S) | `feature-DATA-19` | En cours (ennemis, disciplines et objets faits) |

Origine : décision du porteur du projet du 18/11/2026, après QUAL-09 (interface en anglais, textes du livre encore en français).

## Problème

Avec QUAL-09, l'interface passe en anglais, mais les textes du livre (`data/*.json` : chapitres, disciplines, objets, ennemis) n'existent qu'en français.

## Règle

**Uniquement le texte officiel de la VO** (Project Aon, *Flight from the Dark*, copie locale : `C:\Users\siwer\Desktop\perso\loup solitaire\01fftd (1)\en\xhtml\lw\01fftd\`). Aucune traduction faite par nous : quand la VO n'a pas de texte équivalent, le champ reste vide.

## À faire

Un fichier `data/en/<nom>.json` par fichier français, mêmes `id`, même ordre, mêmes données de jeu ; seuls les textes changent.

1. **`en/ennemi.json`** — fait le 18/11/2026 :
   - `nom` : nom de la ligne de combat de la VO (`<p class="combat">`), section par section, avec sa numérotation (« Giak 1 », « Soldier 2 », « Doomwolf 3 »). Le « Puissant Vordak » du 342 s'appelle « Vordak » dans la VO.
   - `description` : vide (le livre n'en donne pas ; celles du français sont écrites pour l'app).
   - Statistiques de la VO recoupées avec `ennemi.json` : aucun écart.
2. **`en/discipline.json`** — fait le 18/11/2026 : nom et premier paragraphe de la page « Kai Disciplines » (`discplnz.htm`), mot pour mot. Pour Hunting, la règle du Repas est ajoutée telle que la VO la fait écrire sur la Feuille d'Aventure (« Hunting: no need for a Meal when instructed to eat »), comme le français. Weaponskill garde « pick a number… from the list below » de la VO, alors que l'app tire l'arme elle-même.
3. **`en/objet.json`** — fait le 18/11/2026 : `nom` = nom de l'objet en capitales dans la VO (page « Equipment » et sections où on le trouve) ; `description` vide, comme les ennemis ; `categorie` et `effet` inchangés. Correspondances notables : `glaive` = Broadsword (objet de départ n° 0), `sabre` = Short Sword (197, 347), `essence_alether` = Alether (164), `pierres_tombeau` = 20 Tomb Guardian Gems (137), `savon` = Tablet of Perfumed Soap (315), `carte` = Map of Sommerlund. `coin` (Pièce Premium, propre à l'app, absente de la VO) : « Premium Coin », choisi par le porteur du projet.
4. `en/chapitre.json` : à faire (même règle).
5. Chargement en base et choix de la langue côté serveur : modèle de données par langue (avec QUAL-05 ou une tâche dédiée), une fois les quatre fichiers prêts. D'ici là, `GameDataLoader` ignore `data/en/` (hors empreinte DATA-00) : aucun effet sur le jeu.

## Critères d'acceptation

- Chaque `data/en/*.json` a les mêmes entrées et les mêmes données de jeu que le fichier français.
- Tous les textes viennent de la VO, ou sont vides.

## Tests

`DonneesAnglaisesTest` (package `data`) : ennemis — mêmes `id` dans le même ordre, mêmes HABILETÉ, ENDURANCE, résistances et disciplines, nom non vide ; disciplines — mêmes `id` dans le même ordre, nom et description non vides ; objets — mêmes `id`, catégorie et effets, nom non vide. Un test du même type pour chaque fichier suivant.

## Fichiers concernés

`backend/src/main/resources/data/en/*.json`, `backend/src/test/java/.../data/`
