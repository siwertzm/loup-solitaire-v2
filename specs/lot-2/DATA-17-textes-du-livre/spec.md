# DATA-17 — Fautes et écarts dans le texte des chapitres

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-DATA-17` | À faire |

Origine : Audit Claude Code du 11/11/2026 (nouveau) — comparaison complète de `chapitre.json` (sections 1 à 350) avec la VF en ligne chg96 et la VO Project Aon.

## Sources de référence

- VF en ligne : http://chg96.free.fr/loup-solitaire/jouable/lmdt/ (sections `aventure/NNN.htm`). Contient elle-même beaucoup de coquilles (« atterit », « marchant », « Ténébres »…) : elle sert à repérer les écarts, pas à trancher seule.
- VO Project Aon (`01fftd/sect*.htm`) : tranche les écarts de sens (décision D-02).
- PDF VF (fautes connues) : en dernier recours.

## Méthode et limites

1. Comparaison mot à mot du récit, sans les phrases de choix (le site les réécrit en liens).
2. Tous les mots de l'app absents du site ont été relus un par un (choix, sections 0, 2101, 2102 compris).

Ce que la méthode ne voit pas : un mot correct mais faux dans une phrase de choix (ex. « ras » à la place de « bras » n'a été trouvé que dans le récit).

## Fautes de frappe

| Section | Actuel | Correction |
| --- | --- | --- |
| 88 | Prince du Sommer-lund | Prince du Sommerlund |
| 88 | il porte au ras une profonde blessure | il porte au bras une profonde blessure |
| 205 | vous parcourt l'échiné | vous parcourt l'échine |
| 340 | ous galopez (début du texte) | Vous galopez |
| 340 | vore arme prête à frapper | votre arme prête à frapper |
| 21, 2101, 2102 | Si votre cheval s'est enlisé, Utilisez à nouveau | …, utilisez à nouveau |

## HABILETÉ (même décision que QUAL-07)

13 occurrences de `HABILITÉ` / `HABILITE` dans le texte : sections 17, 29, 34, 55, 136, 164, 170, 229, 236, 260, 283 (×2), 342. Les remplacer par `HABILETÉ`. Au 29, corriger aussi « 2 points de HABILITÉ » en « 2 points d'HABILETÉ ». Même faute dans `discipline.json` (Puissance Psychique).

## Écarts de sens (confirmés par la VO)

| Section | Actuel (app) | VO / VF | Correction |
| --- | --- | --- | --- |
| 76 | vous la laissez tomber dans une poche de votre tunique | *slip this Vordak Gem into your Backpack* ; chg96 : « vous la glissez dans votre Sac à Dos » | « …et vous la glissez dans votre Sac à Dos. » |
| 82 | « …gagnez la capitale. » Puis, sentant sa fin proche, vous dites adieu au Prince | *May the luck of the gods ride with you. You bid a sad farewell…* ; chg96 : « Puissent les Dieux vous accompagner au long de votre voyage ! » Le cœur plein de tristesse, vous dites alors adieu au Prince | Rétablir la dernière phrase du Prince ; « sentant sa fin proche, vous » est aussi une faute de construction (c'est le Prince qui meurt). |
| 137 | Elles prennent place dans votre bourse. | *mark these… as a single Backpack Item* | Supprimer la phrase (à faire avec DATA-14, décision D-04). |
| 181 | Vous avez perdu votre Équipement, mais pas la vie. | Absent de la VO et de chg96 | Supprimer la phrase (à faire avec DATA-07, décision D-03). |

## Vérifié sans changement

- 113 : Laumspur à 3 points d'ENDURANCE. L'app est juste ; chg96 dit 4 par erreur.
- Les Repas obligatoires (130, 147, 168, 235, 300) sont bien dans le texte et dans les effets.
- Écarts de style sans faute (chiffres en lettres, « soieries »/« soies », « échelon »/« échelle »…) : laissés tels quels.

## À faire

- Corriger `backend/src/main/resources/data/chapitre.json` (et `discipline.json`).
- La base déjà en production ne relit pas le JSON : ajouter une migration `015-textes-chapitres.sql` avec un `UPDATE … SET text = replace(...)` par correction, sur le modèle de `007-chapitre-272-ponctuation.sql`, avec `rollback`. Si DATA-00 est livré avant, le rechargement du catalogue suffit et la migration est inutile.

## Critères d'acceptation

- `grep -c "HABILIT[EÉ]" chapitre.json discipline.json` = 0.
- Les fautes du tableau ne se retrouvent plus ni dans le JSON ni en base après migration.
- `CoherenceLivreTest` reste vert.

## Tests

- `CoherenceLivreTest.aucuneFauteConnueDansLesTextes` : liste des chaînes interdites (`HABILITÉ`, `Sommer-lund`, `au ras une`, `l'échiné`, `vore arme`, `, Utilisez`) absentes de tous les textes.

## Fichiers concernés

`backend/src/main/resources/data/chapitre.json`, `backend/src/main/resources/data/discipline.json`, `backend/src/main/resources/db/changelog/changes/015-textes-chapitres.sql` (+ `.yaml`, `db.changelog-master.yaml`), `backend/src/test/java/com/loupsolitaire/backend/data/CoherenceLivreTest.java`
