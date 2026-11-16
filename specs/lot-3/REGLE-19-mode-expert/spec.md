# REGLE-19 — Mode expert : Guérison limitée à 10 points au total

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M/L | `feature-REGLE-19` | À faire |

Origine : demande du porteur du projet (16/11/2026), d'après la version « expert » (édition augmentée)

## Problème

L'édition augmentée propose une version « expert » où la Guérison ne peut rendre que **10 points d'ENDURANCE au total**. L'app n'a aucune notion de niveau de difficulté : un seul mode de jeu, sans limite au total de la Guérison.

## Constat

- Aucun mode ni niveau de difficulté dans le code (`Personnage`, création de personnage, API, frontend).
- Guérison actuelle : +1 par chapitre sans combat (`PersonnageService`, fin de `avancerVersChapitre`), sans compteur. REGLE-18 (même lot) restreint en plus les chapitres où elle s'applique.

## À faire (à préciser avec les réponses ci-dessous)

- Choisir le mode (normal / expert) à la création du personnage ; le stocker sur `Personnage` (migration Liquibase, contrainte CHECK si enum, valeur par défaut « normal » pour les personnages existants).
- Compteur des points rendus par la Guérison sur le personnage ; en mode expert, plus de +1 une fois 10 atteint.
- Exposer le mode (et le reste de Guérison en expert) dans `PersonnageResponse` et le modèle TS ; l'afficher (création, fiche, aide).
- Textes : `fr.json`, `en.json`, description de la Guérison, aide du jeu ; `CLAUDE.md` et `backend/README.md`.

## À demander avant de commencer

- **Texte exact** de la règle expert dans l'édition augmentée (10 points pour le tome entier ? par aventure ? par personnage ?).
- **Autres règles du mode expert** dans l'édition augmentée (autres disciplines, repas, combat…) : à inclure ici ou plus tard ?
- **Choix du mode** : à la création seulement, ou modifiable ensuite ? Irréversible une fois l'aventure commencée ?
- **Mort et résurrection** (Pièce Premium) : le compteur est-il remis à zéro, ou conservé ?
- **Affichage** : montrer au joueur les points de Guérison restants (ex. « Guérison 7/10 ») ?
- **Ordre avec REGLE-18** : faire les deux ensemble (même code de Guérison) ?

## Critères d'acceptation

- Personnage expert avec Guérison : après 10 points rendus, plus aucun +1 ; personnage normal : inchangé.
- Personnages existants : mode normal, rien ne change pour eux.

## Tests

- `PersonnageServiceTest` : compteur, plafond de 10 en expert, aucun plafond en normal.
- Test d'intégration de la migration (`*IT`, PostgreSQL) ; Vitest pour le choix du mode et l'affichage.

## Fichiers concernés

`Personnage.java`, migration, `PersonnageService.java`, création de personnage (request/response, `PersonnageMapper`), frontend (création, fiche), `fr.json`, `en.json`, `discipline.json`, `CLAUDE.md`, `backend/README.md`
