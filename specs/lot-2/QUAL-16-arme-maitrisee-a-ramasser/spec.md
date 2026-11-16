# QUAL-16 — Signaler l'arme maîtrisée parmi les objets à ramasser

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `feature-QUAL-16` | À faire |

Origine : retour bêta-testeur (16/11/2026)

## Problème

Quand un chapitre propose une arme (ex. une Épée au 184), le joueur ne voit pas, dans la liste des objets à ramasser, si c'est l'arme de sa Maîtrise des Armes (+2 HABILETÉ). Il doit s'en souvenir ou ouvrir sa fiche.

Constat dans le code :
- `chapitre-objets.component.html` : chaque ligne affiche l'icône, le nom, la quantité restante (`x40` pour l'or) et le bouton PRENDRE ; rien pour la Maîtrise.
- `chapitre-objets.component.ts` a déjà `personnage().armeMaitrisee` (comparé par **nom**, comme dans l'inventaire et le combat) et `bonusArmeMaitrisee = 2`.
- Les clés `CHAPITRE_OBJETS.TAG_MAITRISEE` (« MAÎTRISÉE ») et `CHAPITRE_OBJETS.BONUS_HABILITE` (« +{{bonus}} HAB ») existent déjà dans `fr.json`.
- Chapitres qui proposent une arme (`chapitre.json`) : 15, 20, 62, 148, 184, 197, 243, 255, 267, 290, 291 (×2), 305, 319, 346, 347.

## Décisions (16/11/2026)

- **Libellé** : seulement « MAÎTRISÉE » (clé existante `CHAPITRE_OBJETS.TAG_MAITRISEE`), sans « +2 HAB ».
- **Emplacement** : à côté de la quantité (`x1`), comme le `x40` de l'or ; la quantité reste affichée.
- **Arme déjà possédée** : aucune différence, l'indication s'affiche quand même.
- **Échanges** : oui, même indication sur l'arme proposée par un effet ÉCHANGE (ex. Marteau de l'ermite, onglet Effets).

## À faire

- `chapitre-objets` : dans la ligne d'un objet de catégorie ARME dont le nom est celui de `armeMaitrisee`, afficher « MAÎTRISÉE » à côté de la quantité.
- `chapitre-effets` (bloc ÉCHANGE et sa popup) : même indication sur l'arme proposée en échange. Le composant a déjà `armeMaitrisee(item)` et `personnage().armeMaitrisee`.
- Réutiliser le style du tag MAÎTRISÉE de l'inventaire ; aucun texte en dur (ajouter la clé dans `CHAPITRE_EFFETS` si besoin, et dans `en.json`).
- Valable pour tous les chapitres qui proposent une arme ; rien ne change sans la Discipline Maîtrise des Armes.

## Critères d'acceptation

- Joueur avec Maîtrise des Armes à l'Épée, au 184 : la ligne Épée affiche « MAÎTRISÉE » à côté de `x1`, qu'il ait déjà une Épée ou non.
- Joueur avec Maîtrise des Armes au Marteau de guerre, devant l'échange de l'ermite : l'arme proposée affiche « MAÎTRISÉE ».
- Joueur qui maîtrise l'Épée, devant une autre arme (ex. Lance au 291) : pas d'indication.
- Joueur sans Maîtrise des Armes : aucune indication.

## Tests

Tests Vitest (logique seulement) de `chapitre-objets.component` et `chapitre-effets.component` : arme maîtrisée → indication ; autre arme ou pas de Maîtrise → rien.

## Fichiers concernés

`frontend/src/app/features/game/chapitre/objets/chapitre-objets.component.{ts,html,scss}`, `frontend/src/app/features/game/chapitre/effets/chapitre-effets.component.{ts,html,scss}`, `fr.json` / `en.json` (si nouvelle clé)
