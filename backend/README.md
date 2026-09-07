# Loup Solitaire — Backend V2

## Etat actuel

Ce projet contient pour l'instant uniquement l'entite `Utilisateur` et le
circuit d'authentification JWT complet (register / login / me). Les entites de
jeu (`Joueur`, `Chapitre`, `Combat`...) seront ajoutees dans les prochaines
etapes, en suivant `modele-donnees.md`.

## Prerequis

- Java 17
- PostgreSQL (via Docker ou installation locale) — voir la V1 pour la commande
  `docker run` utilisee precedemment.

## Variables d'environnement

Aucune n'est strictement obligatoire en dev grace aux valeurs par defaut dans
`application.properties`, mais **`JWT_SECRET` doit etre definie explicitement
avant tout deploiement reel** (la valeur par defaut est volontairement
marquee `CHANGE_ME`).

| Variable | Defaut (dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/loup` | URL JDBC PostgreSQL |
| `DB_USERNAME` | `postgres` | Utilisateur DB |
| `DB_PASSWORD` | `postgres` | Mot de passe DB — a definir dans un `.env` local, jamais committe |
| `JWT_SECRET` | secret de dev non securise | Cle Base64 (256 bits mini). Generer avec `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | `3600000` (1h) | Duree de validite du token |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Origine(s) autorisee(s), separees par des virgules |

Exemple pour lancer avec une vraie cle :

```bash
# PowerShell
$env:JWT_SECRET = "<valeur generee par openssl rand -base64 32>"
.\mvnw.cmd spring-boot:run
```

## Lancer le projet

### Option A — Docker (recommande, reproductible)

```bash
# 1. Copier le template d'environnement et remplir les vraies valeurs
cp .env.example .env
# -> ouvrir .env, definir DB_PASSWORD et generer JWT_SECRET avec :
#    openssl rand -base64 32

# 2. Construire et lancer backend + PostgreSQL ensemble
docker compose up --build

# Arreter :
docker compose down
# Arreter et supprimer aussi les donnees Postgres :
docker compose down -v
```

Le backend est alors sur `http://localhost:8080`, la base sur `localhost:5432`.
Les donnees Postgres persistent dans un volume Docker nomme (`loup-db-data`)
entre les redemarrages.

### Option B — Maven local (comme avant, pour du dev rapide sans rebuild Docker)

```bash
# 1. Demarrer uniquement PostgreSQL
docker run --name loup-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=loup -p 5432:5432 -d postgres

# 2. Lancer le backend
./mvnw spring-boot:run       # Linux/Mac
.\mvnw.cmd spring-boot:run    # Windows PowerShell
```

## Tester l'authentification

```bash
# Inscription
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"marius","password":"motdepasse123"}'

# Connexion
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"marius","password":"motdepasse123"}'
# -> { "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer" }

# Route protegee
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer <accessToken recupere ci-dessus>"

# Rafraichir le token d'acces (utilise quand accessToken a expire, apres 15 min)
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken recupere au login>"}'

# Deconnexion (revoque le refresh token)
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

## Changements par rapport a la V1

- ID `Utilisateur` en UUID (au lieu d'un `Long` auto-incremente).
- Secret JWT et identifiants DB externalises en variables d'environnement
  (plus rien en dur dans le code).
- Validation des entrees (`@Valid` + Bean Validation) sur `/register` et `/login`.
- Gestion d'erreurs centralisee (`GlobalExceptionHandler`) : reponses JSON
  structurees avec le bon code HTTP (400/401/404/409) au lieu de 500 generiques.
- `/auth/me` renvoie un DTO (`UtilisateurResponse`) sans jamais exposer le hash
  du mot de passe, contrairement a la V1 qui renvoyait l'entite JPA brute.
- Deploiement Docker : `Dockerfile` multi-stage (build Maven puis image JRE
  legere, utilisateur non-root) + `docker-compose.yml` (backend + PostgreSQL +
  volume persistant + healthchecks).
- CORS ouvert par defaut aux origines natives Capacitor en plus du serveur de
  dev Angular (voir commentaire dans `application.properties`).

## Duree de vie des tokens (decision prise)

- **Token d'acces** (JWT, header `Authorization: Bearer ...`) : 15 minutes.
  Utilise pour chaque appel API normal.
- **Refresh token** (chaine opaque, jamais un JWT) : 30 jours. Stocke cote
  serveur sous forme de hash SHA-256 uniquement (le token en clair n'existe
  que le temps de le transmettre au client). Revocable individuellement
  (`/auth/logout`) ou en masse si une reutilisation apres rotation est
  detectee (vol probable).

### Flux

```
POST /auth/login          -> { accessToken, refreshToken }
... (15 min plus tard, accessToken expire) ...
POST /auth/refresh { refreshToken } -> { accessToken, refreshToken }  (nouveau refreshToken, l'ancien est revoque)
POST /auth/logout { refreshToken }  -> 204, refreshToken revoque
```

Cote app mobile (Ionic/Capacitor) : stocker `refreshToken` dans un stockage
securise (Capacitor Preferences suffit pour du dev ; envisager
`@capacitor-community/secure-storage` pour la prod), intercepter les reponses
401 sur `accessToken` expire et appeler `/auth/refresh` automatiquement avant
de rejouer la requete originale.

