# QUAL-09 — Choix de la langue et en.json corrigé

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Moyenne | M | `feature-QUAL-09` | À faire |

Origine : Audit Claude Code du 11/11/2026 (nouveau). Décision du 11/11/2026 : garder en.json et le rendre utilisable.

## Problème

`app.config.ts` force `lang: 'fr'` et aucun écran ne permet d'en changer : en.json n'est jamais chargé. Il contient pourtant 593 traductions, avec des erreurs que personne ne voit :

| Clé | Actuel | Attendu (édition anglaise Project Aon) |
| --- | --- | --- |
| `INTRO_COMBAT.PUISSANCE_PSYCHIQUE`, `COMBAT_PAGE.TITLE_PUISSANCE_PSYCHIQUE`, `POPUP_PUISSANCE_TITRE`, `POPUP_ENNEMI_PUISSANCE_PSYCHIQUE_*`, `TITLE_ENNEMI_PUISSANCE_PSYCHIQUE`, `POPUP_RESISTANCE_PSYCHIQUE_TEXTE` | Psi-surge (discipline Magnakaï, tomes 6+) | Mindblast |
| `INTRO_INVENTAIRE.OBJETS.MASSE_D_ARMES` | Warhammer | Mace |
| `INTRO_INVENTAIRE.OBJETS.BATON` | Staff | Quarterstaff |
| `ACCUEIL.SOUS_TITRE` | Lone Wolf | Lone Wolf by Joe Dever (le fr cite l'auteur) |
| `CHAPITRE_OBJETS.BADGE_VIDE` | EMPTY | TAKEN (fr : « PRIS ») |
| `INVENTAIRE_SHEET.TITRE_SAC` | BACKPACK | INVENTORY (fr : « INVENTAIRE ») |
| `INVENTAIRE_SHEET.SECTION_OBJETS_REPAS` | ITEMS & MEALS | BACKPACK (fr : « SAC À DOS ») |
| `COMBAT_PAGE.STATS_TITRE` | CHARACTERISTICS | ATTRIBUTES |
| `CHAPITRE_PAGE.LIEN_CONDITION_REQUISE` | Requirement needed | Requirement not met |
| `JOURNAL.ALT_COMBAT` | absente | Combat |

Le nom des disciplines et des objets vient aussi du serveur (`discipline.json`, `objet.json`), en français seulement.

## À faire

1. Langue au démarrage : langue de l'appareil (`navigator.language` ou `@capacitor/device`), `fr` si ce n'est ni `fr` ni `en`. `fallbackLang: 'fr'` reste.
2. Réglage dans l'écran Profil pour changer de langue, mémorisé avec `@capacitor/preferences`. `<html lang>` suit la langue.
3. Corriger en.json (tableau ci-dessus) et ajouter la clé manquante.
4. Décider pour les textes du livre (chapitres, disciplines, objets) : rester en français en v1, ou ajouter les textes anglais côté serveur (lié à QUAL-05 multi-tomes). À trancher avant de commencer.

## Critères d'acceptation

- Un appareil réglé en anglais affiche l'interface en anglais ; en allemand, en français.
- Le choix fait dans le Profil survit à un redémarrage de l'app.
- fr.json et en.json ont les mêmes clés et les mêmes paramètres (test de QUAL-07).

## Tests

- Vitest : la langue initiale vaut `en` pour `en-GB`, `fr` pour `de-DE`.

## Fichiers concernés

`frontend/src/app/app.config.ts`, `frontend/src/app/app.ts`, `frontend/src/app/features/profil/*`, `frontend/public/assets/i18n/en.json`, `frontend/public/assets/i18n/fr.json`
