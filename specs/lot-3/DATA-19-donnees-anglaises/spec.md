# DATA-19 — Jeu de données anglais du livre

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M (par fichier : S) | `feature-DATA-19` | Fait |

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
4. **`en/chapitre.json`** — fait le 18/11/2026 : texte de chaque section de la VO (`sectN.htm`), chapitre 0 = « The Story So Far » (`tssf.htm`), 2101 et 2102 = texte du 21 (découpage propre à l'app, comme en français). Mise au format de l'app : paragraphes séparés par une ligne vide, choix consécutifs sur des lignes qui se suivent, numéro de renvoi en `<strong>`, lignes de combat remplacées par le jeton `[[ENNEMI]]`. Retirés car ajoutés par Project Aon : illustrations et notes de bas de page. Gras ajouté comme en français (décision du porteur du projet) : caractéristiques (COMBAT SKILL, ENDURANCE), noms de disciplines partout ; objets (avec leur quantité, ex. « 6 Gold Crowns ») et ennemis seulement dans les chapitres qui s'en servent (objet trouvé, condition de lien, combat). Les notes de bas de page restent retirées (décision du porteur du projet). Liens, objets, effets, ennemis et `combat` identiques au français.
5. **Chargement en base** — fait le 18/11/2026 :
   - Migration `021-textes-anglais` : colonnes facultatives `chapitre.text_en`, `nom_en` et `description_en` sur `ennemi`, `objet`, `discipline`.
   - `GameDataLoader` lit `data/en/*.json` (dans l'empreinte DATA-00) et remplit ces colonnes ; un `id` anglais inconnu en français fait échouer le démarrage.
   - Langue par requête : en-tête **`X-Langue`** (autorisé dans le CORS), lu par `util/Textes.selon(fr, en)` : anglais si `X-Langue: en` et colonne remplie (même vide), sinon français. Pas `Accept-Language` : les WebView l'envoient d'office avec la langue du téléphone, et les apps déjà installées (interface forcée en français) auraient reçu le livre en anglais.
   - Utilisé par `ChapitreMapper` (texte, ennemis, objets), `CombatMapper`, `PersonnageMapper` (inventaire, arme maîtrisée), `ObjetResponse`, `DisciplineResponse` ; `GET /objets` trié sur le nom dans la langue demandée.
   - Front : `langue.interceptor.ts` envoie `X-Langue` (valeur de `<html lang>`, tenue à jour par `OptionsService`) sur les appels à l'API.
   - Restent en français : export RGPD par email, messages d'erreur du serveur.

## Critères d'acceptation

- Chaque `data/en/*.json` a les mêmes entrées et les mêmes données de jeu que le fichier français.
- Tous les textes viennent de la VO, ou sont vides.

## Tests

`TextesTest` (choix de la langue), `GameDataLoaderTest` (colonnes `*_en` remplies), `ObjetControllerTest` (noms et tri en anglais), `langue.interceptor.spec.ts` ; migration vérifiée par `./mvnw verify` (PostgreSQL).


`DonneesAnglaisesTest` (package `data`) : ennemis — mêmes `id` dans le même ordre, mêmes HABILETÉ, ENDURANCE, résistances et disciplines, nom non vide ; disciplines — mêmes `id` dans le même ordre, nom et description non vides ; objets — mêmes `id`, catégorie et effets, nom non vide ; chapitres — tout identique au français sauf `text`, texte non vide, `[[ENNEMI]]` présent si et seulement si `combat`, chaque renvoi du livre présent en `<strong>N</strong>`. Un test du même type pour chaque fichier suivant.

## Fichiers concernés

`backend/src/main/resources/data/en/*.json`, `backend/src/test/java/.../data/`
