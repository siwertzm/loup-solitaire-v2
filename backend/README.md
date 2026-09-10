# Loup Solitaire — Backend V2

API REST Spring Boot pour une adaptation numérique du livre-jeu **Loup
Solitaire, Livre 1 : Le Seigneur des Ténèbres** (*Flight from the Dark*).
Ce dépôt ne contient que le backend : authentification complète, moteur de
partie (personnage, chapitres, combat, inventaire) et les 350 chapitres du
tome 1 embarqués sous forme de données JSON chargées au démarrage.

## Sommaire

- [Stack technique](#stack-technique)
- [Prérequis](#prérequis)
- [Lancer le projet](#lancer-le-projet)
- [Variables d'environnement](#variables-denvironnement)
- [Authentification](#authentification)
- [Endpoints de l'API](#endpoints-de-lapi)
- [Modèle de jeu (résumé des règles implémentées)](#modèle-de-jeu-résumé-des-règles-implémentées)
- [Format des erreurs](#format-des-erreurs)
- [Tests](#tests)
- [Structure du projet](#structure-du-projet)
- [Limitations connues](#limitations-connues)

## Stack technique

| Composant | Détail |
|---|---|
| Langage / Framework | Java 17, Spring Boot |
| Sécurité | Spring Security, JWT (access + refresh token) |
| Persistance | Spring Data JPA / Hibernate, PostgreSQL |
| Build | Maven (wrapper `mvnw` fourni) |
| Conteneurisation | Docker multi-stage + `docker-compose` (backend + PostgreSQL) |
| Email | Spring Mail (SMTP, ex. Mailtrap en dev) — vérification de compte |
| Données de jeu | JSON statique (`src/main/resources/data/*.json`), chargé en base au démarrage par `GameDataLoader` |

## Prérequis

- Java 17
- PostgreSQL (via Docker ou installation locale)
- Docker + Docker Compose (recommandé, option A ci-dessous)

## Lancer le projet

### Option A — Docker (recommandé, reproductible)

```bash
# 1. Copier le template d'environnement et remplir les vraies valeurs
cp .env.example .env
# -> ouvrir .env, définir DB_PASSWORD et générer JWT_SECRET avec :
#    openssl rand -base64 32

# 2. Construire et lancer backend + PostgreSQL ensemble
docker compose up --build

# Arrêter :
docker compose down
# Arrêter et supprimer aussi les données Postgres :
docker compose down -v
```

Le backend est alors sur `http://localhost:8080`, la base sur
`localhost:5432`. Les données Postgres persistent dans un volume Docker
nommé (`loup-db-data`) entre les redémarrages. Le conteneur backend expose
un healthcheck sur `GET /actuator/health` (utilisé par `docker-compose.yml`
lui-même).

### Option B — Maven local (dev rapide sans rebuild Docker)

```bash
# 1. Démarrer uniquement PostgreSQL
docker run --name loup-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=loup -p 5432:5432 -d postgres

# 2. Lancer le backend
./mvnw spring-boot:run       # Linux/Mac
.\mvnw.cmd spring-boot:run    # Windows PowerShell
```

Au premier démarrage (Option A ou B), `GameDataLoader` charge
automatiquement en base les disciplines, ennemis, objets et les 350
chapitres depuis `src/main/resources/data/*.json`. Ce chargement est
idempotent : il est ignoré si les données sont déjà présentes.

## Variables d'environnement

Aucune n'est strictement obligatoire en dev grâce aux valeurs par défaut
dans `application.properties`, mais **`JWT_SECRET` doit être définie
explicitement avant tout déploiement réel** (la valeur par défaut est
volontairement marquée `CHANGE_ME`).

| Variable | Défaut (dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/loup` | URL JDBC PostgreSQL |
| `DB_USERNAME` | `postgres` | Utilisateur DB |
| `DB_PASSWORD` | `postgres` | Mot de passe DB — à définir dans un `.env` local, jamais committé |
| `DDL_AUTO` | `update` | Stratégie Hibernate (`update` conserve les données ; `create` repart de zéro à chaque démarrage — dev only) |
| `SHOW_SQL` | `true` | Affiche les requêtes SQL générées dans les logs |
| `JWT_SECRET` | secret de dev non sécurisé | Clé Base64 (256 bits mini). Générer avec `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | `900000` (15 min) | Durée de validité du token d'accès |
| `JWT_REFRESH_EXPIRATION_DAYS` | `30` | Durée de validité du refresh token (jours) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,capacitor://localhost,http://localhost,https://localhost` | Origines autorisées, séparées par des virgules (dev Angular + apps natives Capacitor) |
| `MAIL_HOST` | `sandbox.smtp.mailtrap.io` | Hôte SMTP pour l'envoi des emails de vérification |
| `MAIL_PORT` | `2525` | Port SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | *(vide)* | Identifiants SMTP |
| `MAIL_FROM` | `no-reply@loup-solitaire.local` | Adresse expéditeur affichée |
| `APP_BASE_URL` | `http://localhost:8080` | Base de l'URL utilisée dans le lien de confirmation envoyé par email |
| `EMAIL_VERIFICATION_EXPIRATION_HOURS` | `24` | Durée de validité du lien de confirmation d'email |

En dev, utiliser [Mailtrap](https://mailtrap.io) (sandbox gratuite) pour
capturer les emails de vérification sans les envoyer réellement. En
production, remplacer `MAIL_*` par un vrai fournisseur (Resend, SendGrid,
Brevo...) sans toucher au code.

Exemple pour lancer avec une vraie clé en local (Option B) :

```powershell
# PowerShell
$env:JWT_SECRET = "<valeur générée par openssl rand -base64 32>"
.\mvnw.cmd spring-boot:run
```

## Authentification

Toutes les routes sont protégées par un jeton JWT **sauf** `/auth/**` et
`/actuator/health`. Le jeton d'accès s'envoie dans l'en-tête :

```
Authorization: Bearer <accessToken>
```

### Durée de vie des tokens

- **Token d'accès** (JWT) : 15 minutes par défaut. Utilisé pour chaque appel
  API normal.
- **Refresh token** (chaîne opaque, jamais un JWT) : 30 jours par défaut.
  Stocké côté serveur sous forme de hash SHA-256 uniquement (le token en
  clair n'existe que le temps de le transmettre au client). Révocable
  individuellement (`/auth/logout`) ou en masse si une réutilisation après
  rotation est détectée (vol probable).

### Flux

```
POST /auth/register       -> crée le compte, envoie un email de vérification (compte inactif)
GET  /auth/verify-email   -> active le compte (lien cliqué depuis l'email)
POST /auth/login          -> { accessToken, refreshToken }   (échoue si email non vérifié)
... (15 min plus tard, accessToken expiré) ...
POST /auth/refresh { refreshToken } -> { accessToken, refreshToken }  (nouveau refreshToken, l'ancien est révoqué)
POST /auth/logout { refreshToken }  -> 204, refreshToken révoqué
```

### Exemple curl

```bash
# Inscription
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"marius","email":"marius@example.com","password":"motdepasse123"}'

# Connexion (identifiant = username OU email)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifiant":"marius","password":"motdepasse123"}'
# -> { "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer" }

# Route protégée
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer <accessToken>"

# Rafraîchir le token d'accès
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

# Déconnexion (révoque le refresh token)
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

## Endpoints de l'API

Légende : 🔓 public · 🔒 nécessite `Authorization: Bearer <accessToken>`

### Auth (`/auth`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| POST | `/auth/register` | 🔓 | Crée un compte (`username`, `email`, `password`, `dateNaissance` optionnelle). Envoie un email de vérification. `409` si username/email déjà pris. |
| GET | `/auth/verify-email?token=...` | 🔓 | Active le compte depuis le lien reçu par email. Retourne une page HTML simple. |
| POST | `/auth/resend-verification` | 🔓 | Renvoie l'email de vérification (`email`). Réponse identique que l'email existe ou non, pour ne pas divulguer les comptes enregistrés. |
| POST | `/auth/login` | 🔓 | Connexion (`identifiant` = username ou email, `password`). `403` si l'email n'est pas encore vérifié. Retourne `{ accessToken, refreshToken, tokenType }`. |
| POST | `/auth/refresh` | 🔓 | Échange un refresh token valide contre un nouveau couple access/refresh (rotation ; l'ancien refresh token est révoqué). |
| POST | `/auth/logout` | 🔓 | Révoque un refresh token (`refreshToken`). `204 No Content`. |
| GET | `/auth/me` | 🔒 | Profil de l'utilisateur connecté + liste de ses personnages. |
| PUT | `/auth/me` | 🔒 | Met à jour `email` et/ou `dateNaissance` (champs optionnels). Changer d'email repasse `emailVerifie` à `false` et renvoie un email de confirmation. |

### Personnages (`/personnages`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| POST | `/personnages` | 🔒 | Crée un personnage : `nom` + exactement 5 `disciplines` (noms d'enum, ex. `"CAMOUFLAGE"`, `"MAITRISE_ARMES"`). Tire HABILETÉ/ENDURANCE/or/objet de départ, équipe le matériel fixe (Hache, 1 Repas, Carte). |
| GET | `/personnages` | 🔒 | Liste des personnages de l'utilisateur connecté. |
| GET | `/personnages/{id}` | 🔒 | Fiche personnage complète (stats, disciplines, inventaire, chapitre courant). |
| GET | `/personnages/{id}/chapitre` | 🔒 | Chapitre courant : texte, ennemis, effets, liens (avec leurs conditions et leur disponibilité déjà évaluée), objets proposés. Fige un tirage de Table de Hasard (`tirageHasard`) tant que le personnage reste sur ce chapitre. |
| POST | `/personnages/{id}/chapitre/{chapitreCibleId}` | 🔒 | Avance vers `chapitreCibleId` si un lien valide (conditions remplies) existe depuis le chapitre actuel. Réinitialise l'HABILETÉ temporaire. |
| POST | `/personnages/{id}/chapitre/revenir-apres-defaite` | 🔒 | Après une défaite en combat (`Combat.statut = DEFAITE`) : consomme une Pièce Premium (`coin`) pour revenir au chapitre précédent, ENDURANCE totalement restaurée. |
| POST | `/personnages/{id}/ressusciter` | 🔒 | Seule action possible pour un personnage mort hors combat (`mort = true`, ENDURANCE tombée à 0 via un effet de chapitre). Consomme une Pièce Premium, restaure l'ENDURANCE au maximum, reste sur le même chapitre. |
| POST | `/personnages/{id}/objets/{objetId}` | 🔒 | Ramasse un exemplaire d'un objet proposé par le chapitre courant (vérifié côté serveur). |
| DELETE | `/personnages/{id}/objets/{objetId}?quantite=1` | 🔒 | Retire un objet de l'inventaire (debug/test), sans appliquer d'effet de consommation. |
| POST | `/personnages/{id}/objets/{objetId}/consommer` | 🔒 | Consomme un objet (potion, Laumspur...) : applique son effet (ENDURANCE/HABILETÉ) puis le retire de l'inventaire. |
| POST | `/personnages/{id}/vol/{objetId}` | 🔒 | Résout un vol en attente (`volEnAttente` non-null sur la fiche personnage) : choisit quel objet/arme perdre parmi ceux autorisés par la portée du vol. |
| POST | `/personnages/{id}/objets/{objetAAjouterId}/echanger-contre/{objetARetirerId}` | 🔒 | Échange volontaire d'objet, uniquement si le chapitre courant propose explicitement cet échange (ex. Marteau de Guerre de l'ermite). |

### Combat (`/personnages/{id}/combat`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| POST | `/personnages/{id}/combat` | 🔒 | Initie le combat du chapitre courant (doit être `combat = true`), ou renvoie le combat déjà existant (idempotent). |
| GET | `/personnages/{id}/combat` | 🔒 | État du combat en cours (ou du dernier résolu) sur le chapitre actuel. `404` si aucun combat n'a encore été initié. |
| POST | `/personnages/{id}/combat/tour` | 🔒 | Joue un tour. Corps : `{ "action": "ATTAQUE" \| "DEFENSE" \| "OBJET" \| "FUITE", "objetId": "..." }` (`objetId` requis seulement pour `OBJET`). Le serveur fait autorité sur les tirages et les dégâts. |

### Codes retour combat notables

- `Combat.statut` : `EN_COURS`, `VICTOIRE`, `DEFAITE`, `FUITE`, `INTERROMPU` (seuil "assaut échec" du chapitre atteint sans que l'ennemi soit mort).
- `fuitePossible` (dans `CombatResponse`) : indique si le bouton Fuite peut être proposé (condition `FUITE` du chapitre satisfaite).
- Après `DEFAITE`, seule la route `chapitre/revenir-apres-defaite` permet de continuer.

## Modèle de jeu (résumé des règles implémentées)

- **Création de personnage** : HABILETÉ = 10 + tirage(0-9), ENDURANCE = 20 +
  tirage(0-9), exactement 5 disciplines parmi les 10 disciplines Kaï.
- **Discipline Maîtrise des Armes** : tire une arme au hasard parmi le
  catalogue ; +2 HABILETÉ tant qu'elle est équipée.
- **Sans arme équipée** : -4 HABILETÉ.
- **Inventaire** : max 2 armes, max 8 (objets + repas confondus), max 50 pièces
  d'or ; objets spéciaux (armure, clés, carte...) illimités.
- **Repas** : consomme 1 Repas à chaque effet `REPAS` d'un chapitre, sinon
  -3 ENDURANCE ; dispensé si discipline Chasse.
- **Discipline Guérison** : +1 ENDURANCE à chaque chapitre traversé sans
  combat.
- **Discipline Puissance Psychique** : +2 HABILETÉ en combat, sauf contre un
  ennemi qui y résiste.
- **Effets de chapitre** gérés : `ENDURANCE`, `HABILETE` (temporaire, reset à
  chaque changement de chapitre, sauf condition `PERMANENT` qui modifie la
  valeur de base), `VOL` (plusieurs portées : 1 objet au choix, toutes les
  armes, tout le sac, tout), `REPAS`, `ECHANGE`.
- **Combat** : système propre au projet (tables `TABLE_DEGATS_INFLIGES` /
  `TABLE_DEGATS_SUBIS` rééquilibrées, actions ATTAQUE/DEFENSE/OBJET/FUITE) —
  **différent du système officiel du livre** (qui résout chaque round avec un
  seul tirage sur une table unique, sans choix tactique). Voir
  [Limitations connues](#limitations-connues).
- **Mort** : consomme une Pièce Premium (`coin`, objet spécial illimité pour
  l'instant) pour continuer, que ce soit après une défaite en combat ou une
  perte d'ENDURANCE hors combat.

## Format des erreurs

Toutes les erreurs suivent le même format JSON (`ErrorResponse`) :

```json
{
  "timestamp": "2026-09-10T10:15:30Z",
  "status": 400,
  "error": "Requête invalide",
  "message": "Il faut choisir exactement 5 disciplines distinctes"
}
```

| Code HTTP | Cas |
|---|---|
| 400 | Validation des champs (`@Valid`) ou règle métier violée (`IllegalArgumentException`) |
| 401 | Authentification échouée (mauvais identifiants) ou refresh token invalide/expiré |
| 403 | Accès refusé (personnage n'appartenant pas à l'utilisateur) ou compte email non vérifié |
| 404 | Ressource introuvable (personnage, objet, chapitre, utilisateur...) |
| 409 | Conflit (username/email déjà utilisé) ou inventaire plein |

## Tests

```bash
./mvnw test
```

Tests unitaires présents sur : authentification (JWT, filtre, service
utilisateur), `PersonnageService`, `InventaireService`, `ObjetService`,
`EffetChapitreService`, `ConditionService`, `ChapitreMapper`,
`RefreshTokenService`, `EmailService`/`EmailVerificationService`, et le
chargement des données (`GameDataLoaderTest`).

> ⚠️ Pas de test dédié sur `CombatService`, `CombatController` ni
> `TableCombatService` à ce jour — c'est la logique la plus complexe et la
> plus récemment modifiée du projet, à couvrir en priorité.

## Structure du projet

```
backend/src/main/java/com/loupsolitaire/backend/
├── config/          # Sécurité, JWT, CORS, chargement des données de jeu (GameDataLoader)
├── controller/       # AuthController, PersonnageController, CombatController
├── dto/              # DTOs de désérialisation des fichiers JSON du livre
├── exception/        # Exceptions métier + GlobalExceptionHandler
├── model/            # Entités JPA (Personnage, Chapitre, Combat, Objet, Discipline, Ennemi...)
│   └── enums/         # IdDiscipline, CategorieObjet, TypeEffet, TypeCondition, ActionCombat, StatutCombat...
├── repository/       # Repositories Spring Data JPA
├── request/          # DTOs de requête (bodies des endpoints)
├── response/         # DTOs de réponse
└── service/          # Logique métier (Personnage, Inventaire, Objet, Combat, Condition, Effet, Email...)
    ├── mapper/         # Entité -> DTO de réponse
    └── record/         # Records internes (résultats de tour, ajout d'objet...)

backend/src/main/resources/
├── application.properties
└── data/
    ├── chapitre.json     # Les 350 chapitres du Livre 1 (texte, liens, effets, ennemis, objets)
    ├── discipline.json   # Les 10 disciplines Kaï
    ├── ennemi.json        # Le bestiaire (25 ennemis)
    └── objet.json         # Le catalogue d'objets (armes, objets spéciaux, consommables, bourse)
```

## Limitations connues

- **Pas de frontend dans ce dépôt** : uniquement l'API backend. Un client
  (Angular/Ionic ou autre) doit consommer cette API.
- **Système de combat non fidèle au livre** : le livre résout chaque round
  avec un seul tirage sur une table unique (dégâts des deux camps
  simultanément), sans décision tactique. Ce projet ajoute des actions
  ATTAQUE/DEFENSE/OBJET et des tables rééquilibrées maison.
- **Fuite sans risque** : dans le livre, fuir un combat résout quand même le
  round (le joueur encaisse ses dégâts, ceux de l'ennemi sont ignorés).
  Ici, la fuite réussit toujours sans aucun dégât.
- **Chapitre 350 (fin du tome) sans lien de sortie** : son unique lien
  (`351`) est un marqueur de fin de partie filtré au chargement des
  données (`GameDataLoader.PAGES_FIN_DE_JEU`) — il n'existe aucun écran de
  victoire/transition vers le tome 2 pour l'instant.
- **Pièce Premium (`coin`) illimitée** : chaque personnage démarre avec 999
  exemplaires et ils ne sont jamais consommés (MVP1). En pratique, la mort
  (combat ou hors combat) n'a donc actuellement aucune conséquence
  définitive. Un vrai système d'achat/limite est prévu pour une itération
  future (MVP2).
- **`DDL_AUTO=update`** en dev : à remplacer par des migrations
  Flyway/Liquibase avant une mise en production.