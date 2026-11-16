# QUAL-13 — Bande originale (musique de fond)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `feature-QUAL-13` | À faire |

Origine : retour bêta-testeur (16/11/2026)

## Problème

L'app n'a aucun son. Constat dans le code : aucun fichier audio dans `frontend/public` ou `src`, aucun service ni plugin audio (`package.json` : seulement `@capacitor/app`, `core`, `preferences`, `android`, `ios`).

## À faire

Mettre en place le socle d'une bande originale : jouer une musique de fond dans l'app. Le contenu musical lui-même dépend des réponses ci-dessous.

- Un service `core/services/` qui joue, met en pause et coupe la musique (lecture en boucle).
- Fichiers audio dans `frontend/public/assets/`.
- Choix du joueur (musique activée ou non) mémorisé via `@capacitor/preferences`, comme les tokens.
- Textes de l'interface dans `fr.json` (et `en.json`, le test `i18n.spec.ts` exige les mêmes clés).

## À demander avant de commencer

- **Musique** : quels morceaux, d'où viennent-ils (composés, achetés, libres de droits) et sous quelle licence ? Nécessaire avant toute publication sur les stores.
- **Quand** : un seul morceau pour toute l'app, ou un par contexte (accueil, chapitre, combat, mort, victoire) ? Changer selon le chapitre ?
- **Contrôles** : simple bouton activer/couper, ou aussi un réglage de volume ? Les réglages iront dans la page Options (QUAL-15) ; faut-il aussi un bouton rapide dans le jeu ? Musique activée ou coupée par défaut au premier lancement ?
- **Effets sonores** (dés, coups en combat) : dans cette tâche ou plus tard ?
- **Comportement mobile** : couper la musique quand l'app passe en arrière-plan ? Respecter le mode silencieux de l'iPhone ?
- **Technique** : l'élément HTML `<audio>` suffit-il, ou faut-il un plugin Capacitor (lecture native, arrière-plan) ? À trancher avec les réponses ci-dessus.
- **Poids** : taille maximale acceptable des fichiers audio dans l'app (format, qualité).

## Critères d'acceptation

- La musique se lance selon le réglage du joueur et se coupe d'un geste.
- Le choix est conservé après fermeture et réouverture de l'app (web, Android, iOS).
- Aucun texte en dur.

## Tests

Test Vitest du service audio (lecture, coupure, mémorisation du choix), avec l'élément audio et `Preferences` mockés.

## Fichiers concernés

`frontend/src/app/core/services/`, `frontend/public/assets/`, `frontend/public/assets/i18n/fr.json`, `en.json`, la page qui portera le bouton (à décider)
