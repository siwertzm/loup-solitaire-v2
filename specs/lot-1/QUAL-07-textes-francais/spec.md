# QUAL-07 — fr.json : vouvoiement, orthographe et cohérence des textes

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 1 | Moyenne | S | `fix-QUAL-07` | À faire |

Origine : Audit Claude Code du 11/11/2026 (nouveau)

## Problème

L'app vouvoie le joueur partout, comme le livre, sauf à trois endroits. Le nom de la caractéristique est mal orthographié, et plusieurs textes sont en anglais, faux ou incohérents. Tous ces textes sont vus par les testeurs.

### Tutoiement (règle : vouvoiement partout)

| Clé | Actuel | Proposé |
| --- | --- | --- |
| `CHAPITRE_PAGE.POPUP_OBJETS_NON_PRIS_TEXTE` | Si **tu** continues, **tu** ne pourras plus les récupérer. | Si vous continuez, vous ne pourrez plus les récupérer. |
| `CHAPITRE_OBJETS.POPUP_TEXTE` | **Retire** un objet pour faire de la place. | Retirez un objet pour faire de la place. |
| `CREATION_PERSONNAGE.LABEL_TIRAGE_EN_ATTENTE` | **LANCE** LES DÉS | LANCEZ LES DÉS |

### HABILETÉ (décision du 11/11/2026 : orthographe du livre)

« Habilité » veut dire « autorisé ». Le livre écrit **HABILETÉ**. Remplacer `HABILITE` / `habilite` / `Habilite` par `HABILETÉ` / `habileté` / `Habileté` dans **toutes les valeurs** de fr.json (environ 25 clés : `ACCUEIL.HABILITE`, `CREATION_PERSONNAGE.*`, `INTRO_CREATION.*`, `INTRO_EQUIPEMENT.*`, `INTRO_COMBAT.*`, `CHAPITRE_EFFETS.HABILITE_TITRE`, `COMBAT_PAGE.*`, `PERSONNAGE.LABEL_HABILITE`, `INVENTAIRE_SHEET.EFFET_HABILITE`…). Les **clés** ne changent pas. Les abréviations `HAB` restent.

### Anglais ou textes faux

| Clé | Actuel | Proposé |
| --- | --- | --- |
| `AUTH.LOGIN.CHAMP_IDENTIFIANT_LABEL` | Username ou email | Nom d'utilisateur ou e-mail |
| `CHAPITRE_EFFETS.BADGE_FULL_ENDURANCE` | FULL END | END MAX |
| `ACCUEIL.PIED` | Progression sauvegardée sur l'appareil | Progression sauvegardée sur votre compte (la partie est enregistrée sur le serveur, pas sur l'appareil) |
| `CHAPITRE_PAGE.LIEN_ENDURANCE_REQUISE` | {{valeur}} Endurances requises | ENDURANCE {{valeur}} requise |
| `CHAPITRE_PAGE.LIEN_BOURSE_DISPONIBLE` | {{valeur}} {{objet}}s | Pluriel fabriqué en collant un « s » : faux pour « Pièces d'Or », « Repas »… Utiliser une clé par objet ou passer le libellé déjà au pluriel. |
| `INVENTAIRE_SHEET.MAIN_NUE` | Main nue | Mains nues |
| `COMBAT_PAGE.MSG_FUITE` | …disparaissez !! | …disparaissez ! |

### Ponctuation et typographie

- Point final manquant : `INTRO_INVENTAIRE.PARAGRAPHE_1`, `INTRO_INVENTAIRE.PARAGRAPHE_2`, `PROFIL.ERREUR_SUPPRESSION_MDP`, `PROFIL.ERREUR_SUPPRESSION`.
- `COMBAT_PAGE.MSG_PERD_ENDURANCE_GARDE` : retirer le retour à la ligne et les espaces dans la parenthèse → « Vous perdez {{degats}} points d'ENDURANCE (garde : −{{pourcent}} %). »
- Pourcentages : espace insécable avant `%` (`COMBAT_PAGE.TITLE_GARDE`, `POPUP_GARDE_TEXTE`, `MSG_PERD_ENDURANCE_GARDE`).
- Points de suspension : choisir `…` partout (aujourd'hui `...` et `…` sont mélangés, ex. `INTRO_CREATION.CHARGEMENT_DISCIPLINES` et `CREATION_PERSONNAGE.CHARGEMENT_DISCIPLINES`).

### Harmonisation des termes

- « Email », « email », « E-mail », « e-mail » coexistent : choisir **e-mail** (minuscule hors début de phrase).
- « Pièces d'Or » / « Pièces d'or » / « pièces d'or » / « Couronnes » : suivre le livre, **Pièces d'Or** et **Couronnes**.
- « Disciplines Kaï » / « disciplines Kaï » : **Disciplines Kaï**.
- `CHAPITRE_PAGE.LIEN_COURONNES` en minuscules alors que les autres libellés de liens sont en majuscules.

### Hors fr.json

- `CLAUDE.md` : le tome 1 s'appelle *Les Maîtres des Ténèbres* (titre affiché par l'app), pas *Le Seigneur des Ténèbres*.

## À faire

- Appliquer les corrections ci-dessus dans `frontend/public/assets/i18n/fr.json`.
- Reporter l'équivalent dans `en.json` quand le sens change (ex. `ACCUEIL.PIED`) ; le reste de en.json est traité par QUAL-09.

## Critères d'acceptation

- `grep -niE "\b(tu|toi|ton|ta|tes)\b"` sur les valeurs de fr.json ne renvoie rien.
- Aucune valeur de fr.json ne contient `HABILITE` ni `habilite` (hors clés).
- fr.json et en.json ont exactement les mêmes clés et les mêmes paramètres `{{…}}`.

## Tests

- Petit test Vitest (voir QUAL-06) : charge fr.json et en.json, compare la liste des clés et des paramètres, et refuse le tutoiement dans fr.json.

## Fichiers concernés

`frontend/public/assets/i18n/fr.json`, `frontend/public/assets/i18n/en.json`, `CLAUDE.md`
