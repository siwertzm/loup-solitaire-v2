# QUAL-10 — Tests du frontend existant

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | M | `feature-QUAL-10` | À faire |

Origine : demande du 12/11/2026, suite de QUAL-06

## Problème

QUAL-06 a branché `npm test` dans la CI, mais le frontend n'a que deux fichiers de test : `auth.interceptor.spec.ts` (SEC-09) et `i18n.spec.ts` (QUAL-07). Les ~6 800 lignes de `frontend/src/app` (guard, services, pages de jeu) ne sont pas testées.

Le serveur fait autorité sur les règles, mais l'app en recopie une partie pour l'affichage : plafonds d'inventaire, protections contre un malus, objets restant à prendre, bonus d'HABILETÉ affichés en combat. Une divergence avec le backend ne se voit aujourd'hui qu'en jouant : bouton « Prendre » actif alors que le sac est plein, malus annoncé alors que le joueur est protégé, joueur bloqué sur un chapitre.

## À faire

Tests Vitest (`*.spec.ts` à côté du fichier testé), par ordre de priorité. On teste la logique (`computed`, méthodes), pas le rendu HTML ni les styles.

### 1. Socle (session et accès)

- `core/guards/auth.guard.ts` : attend `whenReady()` ; refresh token présent → `true` ; absent → `UrlTree` vers `/auth/login`.
- `core/services/token-storage.service.ts` : hydratation depuis `@capacitor/preferences` (mocké), `setTokens` met à jour la copie synchrone avant la fin de l'écriture, `clear` vide les deux jetons.
- `core/services/auth.service.ts` : `login` et `changePassword` enregistrent les jetons ; `logout` et `supprimerCompte` vident la session ; parcours « mot de passe oublié » (`sessionStorage` : une nouvelle demande efface l'ancien `resetToken`, `resetPassword` nettoie tout).

### 2. Appels API

- `personnage.service.ts`, `chapitre.service.ts`, `combat.service.ts`, `objet.service.ts`, `discipline.service.ts` : avec `HttpTestingController`, chaque méthode appelle la bonne URL, la bonne méthode HTTP et envoie le bon corps (le front n'envoie que des intentions, voir `CLAUDE.md`). Un test par méthode, sans logique supplémentaire.
- `inventaire-sheet.service.ts` : `ouvrir` / `fermer` / `notifierMiseAJour`, et le callback de combat (REGLE-05) remis à `null` à la fermeture.

### 3. Logique de jeu affichée (le plus utile)

- `chapitre-objets.component.ts` :
  - `estPlein` aux plafonds de `CLAUDE.md` : 2 armes, 8 objets + repas cumulés, 50 Pièces d'Or ; objets spéciaux jamais pleins ;
  - `restant`, `itemsDeCategorie` (arme maîtrisée marquée), `categorieObjet` insensible à la casse.
- `chapitre-effets.component.ts` :
  - `effetEvite` : toutes les protections (DISCIPLINE / OBJET) d'un effet doivent être possédées (les cas Torche + Briquet du commentaire) ;
  - `statutRepas` avec et sans Chasse, avec et sans repas ;
  - vol : `itemsEligiblesVol`, `volInteractif`, `volEnAttentePourEffet` ;
  - échange (REGLE-16) : `echangeDejaFait`, `itemsEligiblesEchange`.
- `chapitre.page.ts` :
  - `objetsNonPris` : seuls les gains (`valeur > 0`) avec un restant > 0 ;
  - `segmentsTexte` : découpage autour de `[[ENNEMI]]`, un ennemi par jeton dans l'ordre, texte vide ignoré, jeton sans ennemi ignoré ;
  - `liensEvitementCombat`, `trouverLienRetourNarratif`, `mortAffichee` / `enduranceAffichee`.
- `combat.page.ts` :
  - `habiliteJoueur` et `detailHabiliteAffiche` : arme maîtrisée +2, sans arme −4, Puissance Psychique +2 sauf ennemi qui y résiste, bonus temporaire, bonus de garde ;
  - `ennemiActif`, `pctJoueur` / `pctEnnemi` (bornés à 0-100), `classeBarre*`, `round`, `fuitePossible`.

### 4. Pages d'authentification

- `login.page.ts` : compte non vérifié → bouton « Renvoyer l'e-mail » ; erreur serveur → message traduit, pas le message brut (voir QUAL-08).
- `register.page.ts`, `verify-reset-code.page.ts`, `reset-password.page.ts` : validations du formulaire et redirection après succès.

### Hors périmètre

- Rendu, CSS, animations (machine à écrire du combat, appui long sur un objet).
- Tests de bout en bout (Playwright / Cypress) : à décider plus tard.
- Refactoring : `armesCount`, `objetsCount`, `repasCount`, `bourseCount` sont recopiés dans `chapitre-objets` et `chapitre-effets`. Les tester tels quels ; les regrouper dans une fonction partagée serait une tâche à part.

### Aides de test

- Un fichier `src/testing/fabriques.ts` pour construire un `PersonnageResume`, un `ChapitreResponse`, un `InventaireItem`… avec des valeurs par défaut (évite de recopier de gros objets dans chaque test).
- Pages et composants : `TestBed` avec les services mockés (`useValue`), `TranslateModule.forRoot()` sans chargeur, et on pilote les `signal` d'entrée (`personnage`, `chapitre`, `combat`) directement.

## Critères d'acceptation

- Les parties 1, 2 et 3 sont couvertes ; la partie 4 au minimum pour `login.page.ts`.
- Chaque plafond d'inventaire et chaque bonus d'HABILETÉ de `CLAUDE.md` a un test qui échoue si sa valeur change dans le front.
- `npm test -- --watch=false` est vert en local et dans la CI (`sonar.yml`), en moins d'une minute.
- Optionnel : rapport de couverture (`ng test --coverage`) envoyé à SonarCloud avec `sonar.javascript.lcov.reportPaths`.

## Tests

Cette tâche est elle-même faite de tests. Vérification : casser volontairement une règle (par ex. `maxArmes = 3`) doit faire échouer au moins un test.

## Fichiers concernés

`frontend/src/app/core/**`, `frontend/src/app/features/game/**`, `frontend/src/app/features/auth/**`, `frontend/src/testing/` (nouveau), éventuellement `.github/workflows/sonar.yml` (couverture)
