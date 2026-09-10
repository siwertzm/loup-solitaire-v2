# Loup Solitaire — Frontend

Client Ionic/Angular (web + mobile via Capacitor) pour l'API backend
[loup-solitaire-v2](https://github.com/siwertzm/loup-solitaire-v2).

## Stack technique

| Composant | Détail |
|---|---|
| Framework | Angular 21 (standalone, zoneless) |
| UI | Ionic Angular 9 (standalone components) |
| Mobile | Capacitor 8 (Android + iOS scaffoldés) |
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
│   ├── models/         # DTOs TS alignés sur les request/response Java du backend
│   ├── services/        # AuthService, TokenStorageService
│   ├── interceptors/     # Bearer token + refresh automatique sur 401
│   └── guards/           # authGuard (protège les routes de jeu)
├── features/
│   ├── auth/
│   │   ├── login/        # Formulaire fonctionnel
│   │   └── register/     # Formulaire fonctionnel
│   ├── personnages/
│   │   └── liste/        # Placeholder — à implémenter
│   └── game/
│       └── chapitre/     # Placeholder — à implémenter
├── app.config.ts          # Providers racine (Ionic, Router, HttpClient)
├── app.routes.ts           # Routing lazy-loaded + guard
└── app.ts                   # Shell ion-app / ion-router-outlet
```

## État d'avancement

Fait :

- Scaffold Ionic/Angular + Capacitor (Android/iOS)
- Flux d'authentification complet : register, login, refresh silencieux sur
  401, logout, guard de route
- Stockage des tokens compatible web + natif

À faire (dans l'ordre logique suggéré) :

1. Modèles TS + services HTTP pour `Personnage`, `Chapitre`, `Combat`, `Objet`
   (voir `backend/README.md` > Endpoints de l'API pour le contrat exact)
2. Écran liste + création de personnage (`/personnages`)
3. Écran chapitre courant : texte, liens filtrés, objets proposés, ramassage/échange
4. Écran de combat : tour par tour (`ATTAQUE`/`DEFENSE`/`OBJET`/`FUITE`)
5. Écrans de fin de partie (défaite → `revenir-apres-defaite`, mort → `ressusciter`)

## Notes

- Le token d'accès expire en 15 min par défaut côté backend : l'intercepteur
  gère le refresh automatiquement, aucune action requise dans les composants.
- Si le refresh token est lui-même invalide/expiré/révoqué, l'intercepteur
  déconnecte l'utilisateur et redirige vers `/auth/login`.
