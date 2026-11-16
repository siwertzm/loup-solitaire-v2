# QUAL-17 — Boîte de dialogue du combat trop écrasée quand un dé s'affiche

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Moyenne | S | `fix-QUAL-17` | À faire |

Origine : test du porteur du projet (16/11/2026)

## Problème

Pendant un combat, quand la boîte de dialogue affiche un jet de dé (attaque, riposte), le texte touche la bordure du haut : le contenu ne tient pas dans la hauteur de la boîte.

## Constat dans le code

`combat.page.html` / `combat.page.scss` :
- `.dialogue` a une **hauteur fixe** : `clamp(110px, 21.6vh, 160px)`, moins ses marges et le cadre `.cadre` (bordure 3 px, marges internes).
- Le bloc dé `.de-attaque` empile en colonne, centré verticalement (`justify-content: center`) : le texte `.de-attaque-texte` (15 à 18,5 px, jusqu'à 2 lignes), le libellé `.de-attaque-label`, le dé `.de-attaque-face` (56 à 68 px de côté) et les espaces entre eux.
- Total d'environ 130 à 150 px, plus que la place disponible sur un écran peu haut (≈ 90 à 100 px utiles quand `21.6vh` < 140 px, soit un écran de moins de ~650 px de haut). Le contenu centré déborde des deux côtés : le texte passe sous la bordure du haut.

## Pistes (à choisir)

- **A.** Disposition en ligne pour le dé : texte à gauche, dé (et son libellé) à droite. Tient dans la hauteur actuelle, change peu le rendu.
- **B.** Agrandir la boîte de dialogue quand un dé est affiché (hauteur minimale plus grande), au risque de réduire l'arène du combat au-dessus.
- **C.** Réduire le dé et le texte sur les petits écrans (requête `@media (max-height: …)`).

## À demander avant de commencer

- **Appareil et taille d'écran** où le problème apparaît (modèle de téléphone, ou résolution).
- **Piste préférée** (A, B ou C), ou capture d'écran du rendu voulu.
- **Les autres états de la boîte** (statistiques de l'ennemi, texte long, menu d'actions) débordent-ils aussi sur cet écran ?

## Critères d'acceptation

- Sur l'écran signalé et sur un petit écran de référence (ex. 375 × 667), le texte du dé ne touche plus aucune bordure, le dé reste entièrement visible.
- Pas de changement sur les grands écrans.

## Tests

Vérification visuelle (outils de développement du navigateur, tailles d'écran iPhone SE et appareil signalé) ; pas de test Vitest (rendu CSS, hors périmètre de QUAL-10).

## Fichiers concernés

`frontend/src/app/features/game/combat/combat.page.scss` (et `combat.page.html` si piste A)
