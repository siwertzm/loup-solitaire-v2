# QUAL-08 — Textes en dur et messages du serveur affichés tels quels

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-QUAL-08` | À faire |

Origine : Audit Claude Code du 11/11/2026 (nouveau)

## Problème

La règle « aucune chaîne en dur, tout passe par i18n » (`CLAUDE.md`) n'est pas tenue partout :

1. **Messages du serveur montrés au joueur.** `chapitre.page.ts` (`revenirApresDefaite` et `ressusciter`) affiche `err.error.message`, le texte brut de l'API. Ce texte est sans accents et parle de routes techniques. Exemple réel : « Cette mort vient d'une defaite en combat : ressuscitez-le via POST /personnages/{id}/chapitre/revenir-apres-defaite ».
2. **Textes en dur dans les templates.** `combat.page.html` lignes 61 et 138 : `<span class="etiquette">END</span>`.
3. **Valeurs en dur dans le TS.**
   - `profil.page.ts:55` : nombre de Pièces Premium écrit `"999"` au lieu de la quantité de `coin` renvoyée par l'API. Faux dès que REGLE-15 décomptera les coins.
   - `profil.page.ts:46` : locale `'fr-FR'` fixe pour la date « Membre depuis » ; elle doit suivre la langue active (`TranslateService`).
   - `intro-inventaire.page.ts` : initiales des objets (`'G'`, `'É'`, `'C'`…) écrites en français dans le code ; elles ne suivent pas la langue.
4. **Traductions inutiles.** Environ 25 clés `*.ERREUR_…` ne servent qu'à `console.error(this.translate.instant(...), err)`. Un message de console n'a pas besoin d'être traduit ; ces clés gonflent fr.json et en.json pour rien.

## À faire

- Point 1 : afficher toujours la clé i18n déjà prévue (`CHAPITRE_PAGE.ERREUR_REVENIR_INSTANT`, `ERREUR_RESSUSCITER_INSTANT`), jamais `err.error.message`.
- Point 2 : nouvelle clé `COMBAT_PAGE.ABREVIATION_ENDURANCE` (fr « END », en « END »).
- Point 3 : lire la quantité de `coin` dans les données du compte (ou du personnage) ; utiliser `translate.currentLang` pour la date ; initiales dans fr.json/en.json, ou dérivées du libellé traduit.
- Point 4 : remplacer ces `console.error` par un texte technique fixe (ex. `console.error('chargement personnage', err)`) et supprimer les clés qui ne servent plus, dans les deux fichiers.

## Critères d'acceptation

- `grep -rn "error?.message\|error.message" frontend/src/app` ne renvoie rien.
- Le script d'audit (texte entre balises HTML hors `{{ }}`) ne trouve aucun texte en dur dans `frontend/src/app/**/*.html`.
- Le profil affiche le vrai nombre de coins.

## Tests

- `chapitre.page.spec.ts` : une erreur 400 du serveur sur `ressusciter` affiche le texte de `ERREUR_RESSUSCITER_INSTANT`, pas le message du serveur.

## Fichiers concernés

`frontend/src/app/features/game/chapitre/chapitre.page.ts`, `frontend/src/app/features/game/combat/combat.page.html`, `frontend/src/app/features/profil/profil.page.ts`, `frontend/src/app/features/personnages/inventaire/intro/intro-inventaire.page.ts`, `frontend/public/assets/i18n/fr.json`, `frontend/public/assets/i18n/en.json`
