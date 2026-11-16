# QUAL-14 — Retirer l'id du personnage des URL du jeu

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `feature-QUAL-14` | À faire |

Origine : retour du porteur du projet (16/11/2026)

## Problème

Les URL du jeu contiennent l'UUID du personnage, ex. `http://localhost:4200/personnages/bd7d85e3-3f76-44ac-88a7-5b14e0ea337c/chapitre`. Ce n'est pas joli et ce n'est pas souhaité.

Constat dans le code :
- Routes du frontend (`app.routes.ts`) avec `:id` : `personnage/:id`, `personnages/:id/inventaire/intro`, `personnages/:id/inventaire/depart`, `personnages/:id/chapitre`, `personnages/:id/combat`, `personnages/:id/journal`, `personnages/:id/victoire`.
- Un utilisateur peut avoir plusieurs personnages (`GET /personnages` renvoie une liste) : sans l'id dans l'URL, le front doit savoir quel personnage est en cours.
- L'API (`PersonnageController`, `CombatController`) utilise aussi `/personnages/{id}/...`. Ce n'est pas une faille : `PartieService` vérifie que le personnage appartient à l'utilisateur connecté.
- Dans l'app mobile (Capacitor), l'URL n'est pas visible : le problème ne se voit que sur le web.

## À faire

À préciser avec les réponses ci-dessous. Piste minimale, côté frontend seulement :
- Routes sans id (ex. `partie/chapitre`, `partie/combat`, `partie/journal`...).
- Le personnage en cours est retenu par un service de `core/services/` (signal), et mémorisé via `@capacitor/preferences` pour survivre à un rechargement de page.
- Les pages lisent l'id depuis ce service au lieu de la route. Les appels API ne changent pas.
- Fichiers touchés : `app.routes.ts` et les pages / composants qui lisent ou construisent ces routes (`accueil`, `chapitre`, `chapitre-objets`, `combat`, `inventaire-sheet`, `intro-perso`, `creation-personnage`, `intro-creation`).

## À demander avant de commencer

- **Portée** : seulement les URL affichées dans le navigateur (frontend), ou aussi les routes de l'API (`/personnages/{id}/...`) ? L'exemple donné est une URL du frontend.
- **Nom des routes** : `partie/chapitre`, `jeu/chapitre`, autre ?
- **Plusieurs personnages** : un utilisateur garde-t-il plusieurs personnages ? Si oui, comment choisit-on celui en cours (depuis l'accueil, comme aujourd'hui) ?
- **Rechargement / lien direct** : si la page est rechargée ou ouverte sans personnage en cours mémorisé, renvoyer vers l'accueil ?
- **Anciennes URL** : rediriger les anciennes URL avec id (favoris, liens déjà partagés) ou les laisser tomber sur l'accueil ?

## Critères d'acceptation

- Aucune URL du jeu n'affiche l'id du personnage.
- Recharger une page de jeu garde le personnage en cours (ou suit la décision prise ci-dessus).
- Changer de personnage depuis l'accueil fonctionne toujours.
- `authGuard` toujours présent sur les routes de jeu.

## Tests

Test Vitest du service du personnage en cours (choix, mémorisation, lecture après rechargement) et du comportement sans personnage en cours.

## Fichiers concernés

`frontend/src/app/app.routes.ts`, `frontend/src/app/core/services/`, les pages listées dans « À faire »
