# QUAL-15 — Page Options (paramètres de l'app)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `feature-QUAL-15` | Fait |

Origine : retour du porteur du projet (16/11/2026)

## Problème

L'app n'a pas de page de paramètres. Le joueur ne peut régler ni le son (à venir avec QUAL-13), ni la taille du texte.

Constat dans le code :
- Pas de page d'options dans `app.routes.ts` ; les pages proches sont `profil` et `profil/edition` (compte) et la `nav-bar` du jeu.
- Taille du texte : les 325 `font-size` des `.scss` sont en `px`, aucun en `rem`. Une taille de texte réglable demande donc soit de passer ces tailles en `rem` (relatives à la taille de base), soit une autre technique (zoom global) : à choisir au récap.

## À faire

- Nouvelle page standalone lazy-loadée (ex. `features/options/`), route ajoutée dans `app.routes.ts` avec `authGuard` si elle est réservée aux joueurs connectés (voir questions).
- Un service de `core/services/` qui lit et enregistre les paramètres via `@capacitor/preferences` et les applique au démarrage de l'app.
- **Son** : réglages de QUAL-13 (activer / couper la musique, éventuellement volume). Si QUAL-15 est faite avant QUAL-13, prévoir l'emplacement seulement.
- **Taille du texte** : quelques tailles au choix, appliquées à toute l'app.
- Textes dans `fr.json` et `en.json` (namespace `OPTIONS`).

## À demander avant de commencer

- **Autres paramètres** : liste à définir (« et autre à définir »). Pistes possibles, à valider ou non : langue (QUAL-09), thème clair / sombre, vibrations, vitesse des animations de combat.
- **Accès** : depuis où ouvre-t-on la page (accueil, profil, nav-bar du jeu) ? Accessible sans être connecté ?
- **Taille du texte** : combien de tailles (ex. petite / normale / grande) ou un curseur ? Faut-il suivre le réglage de taille du téléphone ?
- **Technique de la taille du texte** : passer les `font-size` en `rem` (gros changement de styles, mais propre) ou zoom global ? À trancher au récap.
- **Stockage** : paramètres gardés sur l'appareil seulement (`Preferences`), ou liés au compte côté serveur pour les retrouver sur un autre appareil ?
- **Ordre avec QUAL-13** : faire la page Options avant ou après la bande originale ?

## Réalisé (16/11/2026)

- Décisions : taille du texte seulement (langue avec QUAL-09, son avec QUAL-13) ; page `/options` réservée aux joueurs connectés, ouverte depuis le Profil ; trois tailles (petite 0,9 / normale 1 / grande 1,15) ; réglage du téléphone non suivi ; stockage sur l'appareil (`Preferences`).
- Technique **A** : les 325 `font-size` passés en `rem` ; base `:root { font-size: calc(16px * var(--echelle-texte)) }` (prend le pas sur la police dynamique d'Ionic sur iOS) ; le terme en `vw` des `clamp` est multiplié par `--echelle-texte`. Rendu identique en taille normale.
- `OptionsService` (appliqué au démarrage via `provideAppInitializer`), attribut `data-taille-texte` sur `html` pour les ajustements propres à une taille.
- Accès ajouté ensuite : bouton engrenage en haut à gauche de l'accueil, en face du bouton profil ; « Retour » de la page Options revient à la page d'origine (accueil ou profil).
- Retouches en grande taille : bouton « Signaler un bug ou donner un avis » sur une ligne, texte d'accueil sans personnage rééquilibré, titres alignés dans l'inventaire de départ (subgrid).

## Critères d'acceptation

- La page Options est accessible depuis l'endroit décidé.
- Changer la taille du texte s'applique à toute l'app et est conservé après fermeture et réouverture (web, Android, iOS).
- Les réglages du son (une fois QUAL-13 faite) sont dans cette page.
- Aucun texte en dur.

## Tests

Test Vitest du service des paramètres (valeurs par défaut, enregistrement, relecture au démarrage), `Preferences` mocké.

## Fichiers concernés

`frontend/src/app/app.routes.ts`, `frontend/src/app/features/options/` (nouveau), `frontend/src/app/core/services/`, `frontend/src/global.scss` et les `.scss` (taille du texte), `fr.json`, `en.json`
