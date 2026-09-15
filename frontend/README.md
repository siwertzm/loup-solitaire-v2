# Loup Solitaire — Frontend

Client Ionic/Angular (web + mobile via Capacitor) pour l'API backend
[loup-solitaire-v2](https://github.com/siwertzm/loup-solitaire-v2).

Implémente l'intégralité du tome 1 (*Le Seigneur des Ténèbres*) : création de
personnage, navigation dans les 350+ chapitres du livre, combat au tour par
tour, gestion de l'inventaire, mort (narrative / perte d'endurance / combat)
et écran de victoire de fin de tome.

## Stack technique

| Composant | Détail |
|---|---|
| Framework | Angular 21 (standalone, zoneless) |
| UI | Ionic Angular 9 (standalone components) |
| Mobile | Capacitor 8 (Android + iOS scaffoldés) |
| i18n | `@ngx-translate/core` + `http-loader` — tous les textes dans `public/assets/i18n/fr.json`, aucune chaîne en dur dans les composants |
| Stockage tokens | `@capacitor/preferences` (web + natif) |
| HTTP | `HttpClient` + intercepteur JWT avec refresh automatique |

## Prérequis

- Node.js 20+
- Le backend lancé sur `http://localhost:8080` (voir le repo backend)

## Lancer le projet

```bash
npm install --legacy-peer-deps
npm start        # ng serve, http://localhost:4200
```

> `--legacy-peer-deps` est nécessaire : certains peer deps d'Ionic/Capacitor
> ne sont pas encore alignés sur Angular 21 au moment de la création de ce
> projet, alors que le runtime est compatible.

Build de prod :

```bash
npm run build
```

## Mobile (Capacitor)

Les plateformes Android et iOS sont déjà ajoutées (`android/`, `ios/`).
Après chaque changement du build web :

```bash
npm run build
npx cap sync
npx cap open android   # nécessite Android Studio
npx cap open ios       # nécessite Xcode (macOS uniquement)
```

## Configuration

L'URL de l'API se règle dans `src/environments/` :

- `environment.development.ts` → `http://localhost:8080` (utilisé par `ng serve` / `ng build --configuration development`)
- `environment.ts` → URL de prod à adapter au déploiement réel du backend

Le CORS backend autorise déjà `http://localhost:4200` et `capacitor://localhost`
par défaut (`CORS_ALLOWED_ORIGINS`, voir README backend).

## Structure du projet

```
src/app/
├── core/
│   ├── models/            # DTOs TS alignés sur les request/response Java du backend
│   │   ├── auth.model.ts
│   │   ├── personnage.model.ts
│   │   ├── chapitre.model.ts
│   │   └── combat.model.ts
│   ├── services/
│   │   ├── auth.service.ts           # login/register/refresh/logout/changePassword
│   │   ├── token-storage.service.ts  # tokens (web + natif via Capacitor Preferences)
│   │   ├── personnage.service.ts     # CRUD personnage, /me, vol/échange/consommation
│   │   ├── chapitre.service.ts       # GET chapitre courant, avancer
│   │   ├── combat.service.ts         # initier/récupérer combat, jouer un tour
│   │   ├── discipline.service.ts     # catalogue GET /disciplines
│   │   ├── objet.service.ts          # catalogue GET /objets
│   │   └── inventaire-sheet.service.ts  # état partagé de la feuille "SAC À DOS"
│   ├── interceptors/
│   │   └── auth.interceptor.ts       # Bearer token + refresh automatique sur 401
│   └── guards/
│       └── auth.guard.ts             # protège toutes les routes de jeu
│
├── features/
│   ├── auth/
│   │   ├── login/                    # Connexion
│   │   └── register/                 # Inscription (+ écran succès / vérification e-mail)
│   │
│   ├── accueil/                      # Carrousel des personnages
│   │                                  #  - trié par dernier personnage joué (backend)
│   │                                  #  - swipe pour supprimer (vivant ou mort)
│   │                                  #  - carte grisée + bouton RESSUSCITER si mort
│   │                                  #  - carte "TOME 2" désactivée si tome 1 terminé
│   │
│   ├── profil/
│   │   ├── profil.page.ts            # Infos compte, "membre depuis", déconnexion
│   │   └── edition/                  # Modifier username/e-mail, changer mot de passe
│   │
│   ├── personnages/
│   │   ├── creation/
│   │   │   ├── intro/                # Écran d'intro avant la création
│   │   │   └── creation-personnage.page.ts  # Nom, tirage HAB/END, choix des 5 disciplines
│   │   ├── inventaire/
│   │   │   ├── intro/                # Explication de l'équipement de départ
│   │   │   └── inventaire-depart.page.ts    # Révélation animée (dés) de l'équipement tiré
│   │   └── personnage.page.ts        # Fiche personnage (stats, disciplines, suppression, mort)
│   │
│   ├── game/
│   │   ├── chapitre/
│   │   │   ├── chapitre.page.ts      # Écran central : texte, liens conditionnels, onglets
│   │   │   ├── objets/               # Onglet "objets du chapitre" + résumé inventaire
│   │   │   └── effets/               # Onglet "effets" (repas/endurance/habileté/vol/échange)
│   │   ├── combat/
│   │   │   └── combat.page.ts        # Combat tour par tour (arène + boîte de dialogue JRPG)
│   │   └── victoire/
│   │       └── victoire.page.ts      # Écran de fin du tome 1 + teaser Tome 2
│   │
│   ├── regle/                        # Chaîne "Règles du jeu" (4 écrans, lecture seule)
│   │   ├── intro/                    # HABILETÉ / ENDURANCE / mort
│   │   ├── discipline/               # Les 10 Disciplines Kaï (catalogue)
│   │   ├── equipement/               # Armes / sac à dos / objets spéciaux / or / nourriture
│   │   └── combat/                   # Actions, résolution, disciplines en combat, mort au combat
│   │
│   └── shared/
│       ├── nav-bar/                  # Barre basse (CHAPITRE / FICHE / SAC / JOURNAL*)
│       └── inventaire-sheet/         # Feuille "SAC À DOS" globale, montée à la racine
│
├── app.config.ts          # Providers racine (Ionic, Router, HttpClient, ngx-translate)
├── app.routes.ts          # Routing lazy-loaded + guard sur toutes les routes de jeu
└── app.ts                 # Shell ion-app / ion-router-outlet + <app-inventaire-sheet>
```

`*` JOURNAL n'a pas encore d'écran dédié (bouton présent mais désactivé dans `nav-bar`).

## Fonctionnalités

### Authentification & compte
- Inscription, connexion, déconnexion, refresh silencieux du token.
- Modification du profil (username / e-mail), renvoi du lien de vérification.
- Changement de mot de passe.

### Création & fiche personnage
- Tirage animé (dés) de l'HABILETÉ et de l'ENDURANCE, choix de 5 Disciplines Kaï parmi 10.
- Révélation animée de l'équipement de départ (objet aléatoire + Pièces d'Or).
- Fiche personnage : stats, disciplines (avec popup de description), Pièces Premium,
  suppression du personnage (avec confirmation).

### Chapitre (écran central du jeu)
- Affichage du texte du chapitre courant, liens conditionnels (Discipline / Objet /
  Bourse / Hasard / Endurance) résolus et libellés dynamiquement.
- Tirage au hasard animé, figé côté serveur et rejoué à l'identique en cas de retour sur le chapitre.
- Onglet **Objets** : ramassage, complétion d'un objet obligatoire, popup "catégorie
  pleine" pour libérer de la place.
- Onglet **Effets** : effets REPAS / ENDURANCE / HABILETÉ / VOL / ÉCHANGE, avec popups
  de résolution pour les choix du joueur (vol interactif, échange).
- Bouton **VICTOIRE** en fin de tome (chapitre sans lien restant).

### Combat
- Tour par tour (ATTAQUER / DÉFENSE / SAC / FUITE), boîte de dialogue façon JRPG avec
  effet machine à écrire et jets de dés animés.
- Bonus actifs affichés en pictos avec popup d'explication (arme maîtrisée, puissance
  psychique, bouclier psychique, garde, habileté temporaire).
- Combats multi-ennemis : relais immédiat entre adversaires, effet de pile de cartes
  indiquant le nombre d'ennemis restants, animation d'entrée rejouée à chaque nouvel ennemi.
- Coups fatals (mort instantanée) détectés et annoncés par un message dédié plutôt
  qu'un total de dégâts absurde.

### Mort & résurrection
Trois causes de mort, toutes reflétées par `Personnage.mort` mais avec des remèdes
distincts, gérés de façon cohérente sur `ChapitrePage`, `PersonnagePage` et `AccueilPage` :

| Cause | Remède |
|---|---|
| Chapitre de mort narrative | `ressusciter()` → suit le lien de retour ("Pièce Premium") |
| Perte d'endurance (effet/repas) | `ressusciter()` → reste sur le même chapitre |
| Défaite en combat | `revenirApresDefaite()` → recule au chapitre précédent |

Interface toujours grisée de façon cohérente (fiche, accueil, chapitre), sauf l'encart
Pièces Premium, jamais désactivé puisqu'il permet justement de se ressusciter.

### Inventaire
- Feuille "SAC À DOS" globale (armes, objets & repas, objets spéciaux, bourse),
  ouvrable depuis n'importe quel écran de jeu.
- Swipe pour retirer un objet, popup de confirmation pour consommer un consommable.

### Règles du jeu
Chaîne de 4 écrans en lecture seule (intro → disciplines → équipement → combat),
accessibles depuis l'accueil, documentant les règles réelles du jeu.

### Internationalisation
Tous les textes de l'application sont dans `public/assets/i18n/fr.json`, organisés par
namespace (un par page/composant). Les messages dynamiques (dégâts, noms d'objets,
compteurs...) utilisent l'interpolation `ngx-translate` (`translate:{ param }`).

## Notes

- Le token d'accès expire en 15 min par défaut côté backend : l'intercepteur
  gère le refresh automatiquement, aucune action requise dans les composants.
- Si le refresh token est lui-même invalide/expiré/révoqué, l'intercepteur
  déconnecte l'utilisateur et redirige vers `/auth/login`.
- Le combat côté backend fait autorité sur chaque tirage : le frontend n'affiche
  que les nombres renvoyés par l'API, il ne calcule jamais de dégâts lui-même.

## À faire

- Écran **JOURNAL** : pas encore d'écran dédié, bouton désactivé dans `nav-bar`
  (contrairement à **FICHE**, qui pointe déjà vers un vrai écran, `/personnage/:id`).
- Tome 2 (*La Traversée Infernale*) : seule une carte "à venir" est affichée sur
  l'accueil pour un personnage ayant terminé le tome 1.