# Loup Solitaire — Backend V2

API REST Spring Boot pour une adaptation numérique du livre-jeu **Loup
Solitaire, Tome 1 : Les Maîtres des Ténèbres** (*Flight from the Dark*).
Ce dossier contient le backend du monorepo (le client Ionic/Angular est dans
`../frontend`) : authentification complète, moteur de partie (personnage,
chapitres, combat, inventaire) et les 350 chapitres du tome 1 embarqués sous
forme de données JSON chargées au démarrage. Les tâches en cours sont dans
`../specs/README.md`.

## Sommaire

- [Stack technique](#stack-technique)
- [Prérequis](#prérequis)
- [Lancer le projet](#lancer-le-projet)
- [Variables d'environnement](#variables-denvironnement)
- [Authentification](#authentification)
- [Endpoints de l'API](#endpoints-de-lapi)
- [Modèle de jeu (résumé des règles implémentées)](#modèle-de-jeu-résumé-des-règles-implémentées)
- [Format des erreurs](#format-des-erreurs)
- [Sauvegardes et restauration (OPS-06)](#sauvegardes-et-restauration-ops-06)
- [Tests](#tests)
- [Structure du projet](#structure-du-projet)
- [Limitations connues](#limitations-connues)

## Stack technique

| Composant | Détail |
|---|---|
| Langage / Framework | Java 21, Spring Boot |
| Sécurité | Spring Security, JWT (access + refresh token) |
| Persistance | Spring Data JPA / Hibernate (`ddl-auto=validate`), PostgreSQL, schéma géré par Liquibase (`db/changelog/`) |
| Build | Maven (wrapper `mvnw` fourni) |
| Conteneurisation | Docker multi-stage + `docker-compose` (backend + PostgreSQL) |
| Email | Spring Mail (SMTP, ex. Mailtrap en dev) — vérification de compte |
| Données de jeu | JSON statique (`src/main/resources/data/*.json`), chargé en base au démarrage par `GameDataLoader` |

## Prérequis

- Java 21
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
# 1. Démarrer uniquement PostgreSQL (la première fois)
docker run --name loup-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=loup -p 5432:5432 -d postgres
#    Les fois suivantes (Docker Desktop lancé) : docker start loup-db

# 2. Créer backend/.env (une seule fois) avec au minimum :
#    DB_PASSWORD=postgres
#    JWT_SECRET=<valeur générée par : openssl rand -base64 32>
#    (le backend lit ce fichier tout seul quand il est lancé depuis backend/)

# 3. Lancer le backend
./mvnw spring-boot:run       # Linux/Mac
.\mvnw.cmd spring-boot:run    # Windows PowerShell
```

Au premier démarrage (Option A ou B), `GameDataLoader` charge
automatiquement en base les disciplines, ennemis, objets et les 353
chapitres depuis `src/main/resources/data/*.json`.

**Les JSON font foi (DATA-00).** Leur empreinte SHA-256 est gardée dans la
table `catalogue_empreinte`. À chaque démarrage :

- empreinte identique → rien n'est rechargé (« Catalogue du livre a jour ») ;
- empreinte différente ou absente → tout le catalogue est remis à l'identique
  des JSON, dans une seule transaction : mise à jour par identifiant, liens,
  conditions, effets, objets et ennemis des chapitres recréés. Les données
  des joueurs ne référencent que des identifiants stables (chapitre, objet,
  ennemi, discipline) et ne sont pas touchées.

Le cache de second niveau d'Hibernate (OPS-07) est entièrement vidé au début
d'un rechargement, collections comprises, pour ne pas relire d'anciens liens
ou effets supprimés en base.

Corriger le livre = modifier le JSON puis redéployer, **sans migration de
données**. Une entrée retirée d'un JSON reste en base (un joueur peut
l'avoir dans son inventaire) et est signalée dans les logs.

Avant d'enregistrer l'empreinte, chaque chapitre doit avoir au moins un lien
(sauf le 350, fin du tome) ; sinon la transaction est annulée et le
démarrage échoue (OPS-03). Pour forcer un rechargement :
`update catalogue_empreinte set empreinte = 'a-recharger';` puis redémarrer.

## Mémoire et charge (OPS-01)

Render : runtime Docker (`backend/Dockerfile`), instance 1 CPU / 2 Go,
health check `/actuator/health`.

- La JVM est lancée avec `-XX:MaxRAMPercentage=75` (tas limité à 1,5 Go sur
  2 Go, le reste pour la JVM elle-même) et `-XX:+ExitOnOutOfMemoryError` :
  plus de mémoire → arrêt net, Render redémarre le service.
- Sondes publiques : `/actuator/health/liveness` (l'application tourne) et
  `/actuator/health/readiness` (elle accepte du trafic), en plus de
  `/actuator/health`.

Mesure du 12/11/2026, image Docker lancée avec les limites de Render
(`--cpus=1 -m 2g`), joueurs simulés par `scripts/charge.py` (compte,
personnage, chapitres, combats, résurrections) :

| Charge | Requêtes/s | Médiane / p95 | Mémoire max | Résultat |
|---|---|---|---|---|
| 50 joueurs, 3 min, ~1 s de réflexion | 165 | 4 ms / 39 ms | 455 Mo / 2 Go | aucune erreur 5xx, pas d'arrêt |
| 50 joueurs, 1 min, ~0,2 s de réflexion | 525 (CPU à 100 %) | 25 ms / 83 ms | 451 Mo / 2 Go | aucune erreur 5xx, pas d'arrêt |

La limite est le CPU, pas la mémoire. Pour relancer la mesure, voir l'en-tête
de `scripts/charge.py` (base de test, limitation de débit et e-mails
désactivés côté serveur ; jamais contre la production).

## Purge des jetons (OPS-05)

`PurgeService` tourne chaque jour à 4 h, heure de Paris (`PURGE_CRON`,
format cron Spring) et supprime :

- les sessions (`refresh_token`) expirées depuis plus de 7 jours ; les
  sessions révoquées mais pas encore expirées sont gardées (détection de
  réutilisation, SEC-04) ;
- les liens de vérification d'e-mail expirés ;
- les demandes « mot de passe oublié » dont le code et l'étape de
  réinitialisation ont expiré.

Les **comptes ne sont jamais supprimés**, même non vérifiés (décision du
12/11/2026) : leur lien expiré disparaît, « Renvoyer le lien » en recrée
un. Le nombre de lignes supprimées est écrit dans les logs (`Purge : …`).

## Sauvegardes et restauration (OPS-06)

### Plan Render

Base PostgreSQL sur un plan payant **Basic-1gb** (16/11/2026). À vérifier dans
l'onglet *Recovery* de la base, Render pouvant changer ses conditions :

- **pas d'expiration** (contrairement aux bases gratuites, supprimées au bout de 30 jours) ;
- **restauration à un instant donné** (*Point-in-Time Recovery*) sur les derniers
  jours (environ 3 jours en espace Hobby, 7 en Professional) : elle crée une
  **nouvelle** base, l'ancienne n'est pas touchée ;
- **exports manuels** (*logical backups*) téléchargeables, à lancer depuis *Recovery*.

Aucune sauvegarde hors de Render. Piste écartée pour l'instant : un `pg_dump`
nocturne par GitHub Action, stocké ailleurs (protège d'une perte du compte
Render, mais met l'URL de la base en secret GitHub et des données personnelles
dans les dumps : stockage et durée de conservation à décider, RGPD).

### Avant chaque déploiement qui contient une migration

1. *Recovery* → **Create export**, puis télécharger le fichier (hors du dépôt Git).
2. Noter l'heure du déploiement : en cas de problème, on restaure à la minute d'avant.

### Restaurer

**A. Instant donné, chez Render** (cas normal : migration ou donnée cassée)

1. Base → *Recovery* → *Restore to a point in time* : choisir l'heure juste avant
   l'incident. Render crée une nouvelle base.
2. Service backend → *Environment* : remplacer `DB_URL` (URL **interne** de la
   nouvelle base, au format `jdbc:postgresql://hôte:5432/nom`), `DB_USERNAME`,
   `DB_PASSWORD`.
3. Redéployer **le commit qui allait avec cette base** (sinon Liquibase rejoue
   les migrations du code récent).
4. Vérifier : `/actuator/health` à `UP`, connexion avec un compte existant,
   un personnage s'ouvre.
5. Garder l'ancienne base quelques jours, puis la supprimer.

**B. Depuis un export** (`pg_dump -Fc`)

```bash
createdb -h <hote> -U <utilisateur> loup_restauree
pg_restore -h <hote> -U <utilisateur> -d loup_restauree --no-owner export.dump
```

puis étapes 2 à 5 de A. En local, avec le conteneur Docker :

```bash
docker exec loup-db pg_dump -U postgres -d loup -Fc -f /tmp/loup.dump
docker exec loup-db psql -U postgres -c "create database loup_restauree"
docker exec loup-db pg_restore -U postgres -d loup_restauree --no-owner /tmp/loup.dump
```

**C. Annuler une seule migration** : chaque changeset a son `rollback` (bloc
`rollback:` du `.yaml`). Le projet n'a pas le plugin Maven de Liquibase : on
exécute ce SQL à la main (`psql`), on retire la ligne du changeset
(`delete from databasechangelog where id = 'NNN-nom';`), puis on redéploie le
code précédent. À réserver aux migrations de structure : une migration qui supprime
des données (ex. 013, date de naissance) ne se rattrape qu'avec A ou B.

### Durées mesurées

| Date | Où | Taille | Sauvegarde | Restauration | Redémarrage + vérification | Résultat |
|---|---|---|---|---|---|---|
| 16/11/2026 | local (Docker, PostgreSQL 18, 27 tables, 353 chapitres) | dump 155 Ko | `pg_dump` 0,25 s | `pg_restore` 0,26 s | 9 s (Liquibase à jour, catalogue reconnu, `health` UP) | comptages identiques sur les 27 tables |
| *à faire* | Render (instant donné) | | | | | |

**Une fois par trimestre** : refaire une restauration A sur une base de test,
compléter le tableau, puis supprimer la base de test.

### Règle des migrations

Une migration doit rester **rétrocompatible** : le code de la version
précédente doit encore tourner après elle (pour pouvoir revenir au code
d'avant sans restaurer la base). Pour renommer ou supprimer : **ajouter** la
nouvelle colonne, **migrer** les données et le code, puis **supprimer**
l'ancienne dans une version suivante. Chaque changeset a son `rollback`
(voir `CLAUDE.md`).

## Failles connues des dépendances (SEC-07)

La CI (`sonar.yml`) bloque une PR quand une dépendance livrée a une faille
connue (CVE) de gravité haute ou critique **déjà corrigée** par une version :

- frontend : `npm audit --omit=dev --audit-level=high` (les outils de
  développement ne sont pas livrés et ne sont pas contrôlés) ;
- backend : [Trivy](https://trivy.dev) scanne le jar de production
  (`docker run aquasec/trivy:0.58.1 rootfs ... target/`).

Une PR peut donc échouer parce qu'une faille vient d'être publiée, même sans
rapport avec la PR : la correction est presque toujours une montée de
version (Spring Boot, Angular…). Dependabot (`.github/dependabot.yml`)
ouvre chaque semaine une PR de mise à jour par écosystème (Maven, npm,
GitHub Actions). Pour vérifier en local :

```bash
./mvnw package -DskipTests
docker run --rm -v "$PWD/target":/scan aquasec/trivy:0.58.1 rootfs --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed /scan
```

H2 n'est utilisé que par les tests (scope `test`) : il n'est pas dans le jar.

## Variables d'environnement

**`JWT_SECRET` et `DB_PASSWORD` sont obligatoires, en local comme en
production** : ils n'ont aucune valeur par défaut et l'application refuse
de démarrer s'ils manquent (SEC-03). Une clé JWT invalide, trop courte ou
déjà publiée dans le dépôt est aussi refusée au démarrage.

- En local : les mettre dans `backend/.env` (voir `.env.example`), lu à la
  fois par `docker compose` et par `mvnw spring-boot:run` lancé depuis
  `backend/`.
- Sur Render : les définir dans les variables d'environnement du service
  **avant** de déployer.

Les autres variables ont une valeur par défaut adaptée au dev.

| Variable | Défaut (dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/loup` | URL JDBC PostgreSQL |
| `DB_USERNAME` | `postgres` | Utilisateur DB |
| `DB_PASSWORD` | **aucun (obligatoire)** | Mot de passe DB — dans `backend/.env` en local, jamais committé |
| `DDL_AUTO` | `validate` | Stratégie Hibernate. Le schéma est créé par Liquibase : Hibernate vérifie seulement qu'il correspond aux entités |
| `SHOW_SQL` | `false` | Affiche les requêtes SQL générées dans les logs |
| `JWT_SECRET` | **aucun (obligatoire)** | Clé Base64 (256 bits mini). Générer avec `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | `900000` (15 min) | Durée de validité du token d'accès |
| `JWT_REFRESH_EXPIRATION_DAYS` | `30` | Durée de validité du refresh token (jours) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,capacitor://localhost,http://localhost,https://localhost` | Origines autorisées, séparées par des virgules (dev Angular + apps natives Capacitor) |
| `MAIL_HOST` | `sandbox.smtp.mailtrap.io` | Hôte SMTP pour l'envoi des emails de vérification |
| `MAIL_PORT` | `2525` | Port SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | *(vide)* | Identifiants SMTP |
| `MAIL_FROM` | `no-reply@loup-solitaire.local` | Adresse expéditeur affichée |
| `APP_BASE_URL` | `http://localhost:8080` | Base de l'URL utilisée dans le lien de confirmation envoyé par email |
| `EMAIL_VERIFICATION_EXPIRATION_HOURS` | `24` | Durée de validité du lien de confirmation d'email |
| `SENTRY_DSN` | *(vide = désactivé)* | Clé du projet Sentry backend : chaque erreur 500 y est envoyée (OPS-04) |
| `SENTRY_ENVIRONMENT` | `local` | Environnement affiché dans Sentry (`production` sur Render) |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | *(vide = texte)* | `ecs` sur Render : logs au format JSON, faciles à filtrer (OPS-04) |
| `APP_VERSION_MINIMALE` | `1.0` | Version minimale de l'app mobile (`GET /version`, QUAL-02). L'augmenter sur Render force la mise à jour des apps plus anciennes, sans redéployer |
| `SPRINGDOC_ENABLED` | `false` | `true` = description OpenAPI sur `/v3/api-docs` et Swagger UI sur `/swagger-ui.html` (à laisser à `false` en production) |
| `DIAGNOSTIC_ERREUR_TEST` | `false` | `true` le temps d'un test : `GET /diagnostic/erreur-test` renvoie une 500 volontaire pour vérifier l'alerte Sentry |

En dev, utiliser [Mailtrap](https://mailtrap.io) (sandbox gratuite) pour
capturer les emails de vérification sans les envoyer réellement. En
production, remplacer `MAIL_*` par un vrai fournisseur (Resend, SendGrid,
Brevo...) sans toucher au code.

Sans `.env`, on peut aussi passer les variables dans le terminal (Option B) :

```powershell
# PowerShell
$env:DB_PASSWORD = "postgres"
$env:JWT_SECRET = "<valeur générée par openssl rand -base64 32>"
.\mvnw.cmd spring-boot:run
```

## Authentification

Toutes les routes sont protégées par un jeton JWT **sauf** `/auth/**` (hors
`/auth/me` et `/auth/me/**`, qui exigent le jeton), `/actuator/health`,
`/version`, les pages légales (`/legal/**`) et, si activés, Swagger et
`/diagnostic/erreur-test`. Le jeton d'accès s'envoie dans l'en-tête :

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

**Préfixe `/api/v1` (QUAL-02)** : toutes les routes répondent aussi sous `/api/v1` (ex. `/api/v1/auth/login`), que l'app utilise. Les chemins sans préfixe restent servis pour les apps déjà installées ; `PrefixeApiFilter` retire le préfixe avant la sécurité, les règles sont donc les mêmes.

### Version (`/version`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| GET | `/version` | 🔓 | `{ versionMinimale }` (QUAL-02). Au démarrage et à chaque retour au premier plan, l'app compare sa version (`versionName` Android, `MARKETING_VERSION` iOS) : plus ancienne → écran « Mise à jour requise ». Pour forcer une mise à jour : publier la nouvelle version dans les stores, puis augmenter `APP_VERSION_MINIMALE` sur Render. |

### Catalogue et pages légales

| Méthode | Route | Accès | Description |
|---|---|---|---|
| GET | `/disciplines` | 🔒 | Les 10 Disciplines Kaï. |
| GET | `/objets` | 🔒 | Catalogue des objets (catégorie, effets). |
| GET | `/legal/cgu.html`, `/legal/confidentialite.html` | 🔓 | CGU et politique de confidentialité (`static/legal/`). |

### Auth (`/auth`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| POST | `/auth/register` | 🔓 | Crée un compte (`username`, `email`, `password`) et envoie un email de vérification. Répond toujours `202` sans contenu, que l'email ait déjà un compte ou non (SEC-05) : dans ce cas aucun compte n'est créé et un email part à cette adresse (rappel « tu as déjà un compte », ou nouveau lien si le compte n'est pas vérifié ; 3 par heure au plus). `409` seulement si le **nom d'utilisateur** est pris. |
| GET | `/auth/verify-email?token=...` | 🔓 | Active le compte depuis le lien reçu par email, ou remplace l'email du compte par l'email en attente (SEC-06). Redirige vers l'app. `409` si l'adresse en attente a été prise entre-temps par un autre compte. |
| POST | `/auth/resend-verification` | 🔓 | Renvoie l'email de vérification (`email`). Réponse identique que l'email existe ou non, pour ne pas divulguer les comptes enregistrés. |
| POST | `/auth/login` | 🔓 | Connexion (`identifiant` = username ou email, `password`). `403` si l'email n'est pas encore vérifié. Retourne `{ accessToken, refreshToken, tokenType }`. |
| POST | `/auth/refresh` | 🔓 | Échange un refresh token valide contre un nouveau couple access/refresh (rotation ; l'ancien refresh token est révoqué). |
| POST | `/auth/logout` | 🔓 | Révoque un refresh token (`refreshToken`). `204 No Content`. |
| POST | `/auth/forgot-password` | 🔓 | Envoie un code de réinitialisation par email (`email`). Réponse `202` identique que l'adresse existe ou non. |
| POST | `/auth/verify-reset-code` | 🔓 | Vérifie le code reçu (`email`, `code`) et renvoie un `resetToken`. |
| POST | `/auth/reset-password` | 🔓 | Nouveau mot de passe (`resetToken`, `newPassword`). `204`. |
| GET | `/auth/me` | 🔒 | Profil de l'utilisateur connecté + liste de ses personnages. |
| PUT | `/auth/me` | 🔒 | Met à jour `username` et/ou `email` (champs optionnels). Changer d'email exige `motDePasseActuel` (`401` sinon) et ne change pas l'email du compte : le nouveau est gardé dans `emailEnAttente`, un lien de confirmation part à la nouvelle adresse et une alerte à l'ancienne. L'ancien email sert à se connecter jusqu'au clic sur le lien (SEC-06). |
| PUT | `/auth/me/password` | 🔒 | Change le mot de passe (`currentPassword`, `newPassword`), renvoie un nouveau couple de jetons. |
| POST | `/auth/me/export` | 🔒 | RGPD-02 : envoie toutes les données du compte (profil, personnages, inventaires, journaux, combats) en pièce jointe JSON à l'email **vérifié** du compte. `202` ; `400` si l'email n'est pas vérifié ; `429` au-delà de 3 exports par 24 h. Mot de passe et jetons exclus. |
| DELETE | `/auth/me` | 🔒 | Supprime définitivement le compte, après vérification du mot de passe (`password`). `204`. |

### Personnages (`/personnages`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| POST | `/personnages/tirage` | 🔒 | Tire HABILITE et ENDURANCE côté serveur. Appels répétés : toujours le même tirage, jusqu'à la création du personnage. |
| POST | `/personnages` | 🔒 | Crée un personnage : `nom` + exactement 5 `disciplines` (noms d'enum, ex. `"CAMOUFLAGE"`, `"MAITRISE_ARMES"`). Utilise le tirage en attente (`400` sans tirage), tire or et objet de départ, équipe le matériel fixe (Hache, 1 Repas, Carte). |
| GET | `/personnages` | 🔒 | Liste des personnages de l'utilisateur connecté, le plus récemment joué en tête. |
| DELETE | `/personnages/{id}` | 🔒 | Supprime le personnage. `204`. |
| GET | `/personnages/{id}/historique` | 🔒 | Journal : chapitres traversés, du plus récent au plus ancien. |
| GET | `/personnages/{id}` | 🔒 | Fiche personnage complète (stats, disciplines, inventaire, chapitre courant). |
| GET | `/personnages/{id}/chapitre` | 🔒 | Chapitre courant : texte, ennemis, effets, liens (avec leurs conditions et leur disponibilité déjà évaluée), objets proposés. Fige un tirage de Table de Hasard (`tirageHasard`) tant que le personnage reste sur ce chapitre. |
| POST | `/personnages/{id}/chapitre/{chapitreCibleId}` | 🔒 | Avance vers `chapitreCibleId` si un lien valide (conditions remplies) existe depuis le chapitre actuel. Réinitialise l'HABILITE temporaire. |
| POST | `/personnages/{id}/chapitre/revenir-apres-defaite` | 🔒 | Après une défaite en combat (`Combat.statut = DEFAITE`) : exige une Pièce Premium (`coin`, non consommée en MVP1) pour revenir au chapitre précédent, ENDURANCE totalement restaurée. |
| POST | `/personnages/{id}/ressusciter` | 🔒 | Seule action possible pour un personnage mort hors combat (`mort = true`, ENDURANCE tombée à 0 via un effet de chapitre). Exige une Pièce Premium (non consommée en MVP1), restaure l'ENDURANCE au maximum, reste sur le même chapitre. |
| POST | `/personnages/{id}/objets/{objetId}` | 🔒 | Ramasse un exemplaire d'un objet proposé par le chapitre courant (vérifié côté serveur). |
| DELETE | `/personnages/{id}/objets/{objetId}?quantite=1` | 🔒 | Retire un objet de l'inventaire (debug/test), sans appliquer d'effet de consommation. |
| POST | `/personnages/{id}/objets/{objetId}/consommer` | 🔒 | Consomme un objet (potion, Laumspur...) : applique son effet (ENDURANCE/HABILITE) puis le retire de l'inventaire. |
| POST | `/personnages/{id}/vol/{objetId}` | 🔒 | Résout un vol en attente (`volEnAttente` non-null sur la fiche personnage) : choisit quel objet/arme perdre parmi ceux autorisés par la portée du vol. |
| POST | `/personnages/{id}/objets/{objetAAjouterId}/echanger-contre/{objetARetirerId}` | 🔒 | Échange volontaire d'objet, uniquement si le chapitre courant propose explicitement cet échange (ex. Marteau de Guerre de l'ermite), et une seule fois par passage sur le chapitre (REGLE-16). |

### Combat (`/personnages/{id}/combat`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| POST | `/personnages/{id}/combat` | 🔒 | Initie le combat du chapitre courant (doit être `combat = true`), ou renvoie le combat déjà existant (idempotent). |
| GET | `/personnages/{id}/combat` | 🔒 | État du combat en cours (ou du dernier résolu) sur le chapitre actuel. `404` si aucun combat n'a encore été initié. |
| POST | `/personnages/{id}/combat/tour` | 🔒 | Joue un tour. Corps : `{ "action": "ATTAQUE" \| "DEFENSE" \| "OBJET" \| "FUITE", "objetId": "..." }` (`objetId` requis seulement pour `OBJET`). Le serveur fait autorité sur les tirages et les dégâts. |

### Codes retour combat notables

- `Combat.statut` : `EN_COURS`, `VICTOIRE`, `DEFAITE`, `FUITE`, `INTERROMPU` (seuil "assaut échec" du chapitre atteint sans que l'ennemi soit mort).
- `fuitePossible` (dans `CombatResponse`) : indique si le bouton Fuite peut être proposé (seuil d'assauts d'une condition `FUITE` atteint, ou lien `EVITEMENT` sur le chapitre : fuite dès la 1re action).
- Après `DEFAITE`, seule la route `chapitre/revenir-apres-defaite` permet de continuer.

## Modèle de jeu (résumé des règles implémentées)

- **Création de personnage** : HABILITE = 10 + tirage(0-9), ENDURANCE = 20 +
  tirage(0-9), exactement 5 disciplines parmi les 10 disciplines Kaï.
  Au plus 20 personnages par compte (SEC-12) : au-delà, tirage et création
  répondent `400`.
- **Discipline Maîtrise des Armes** : l'arme est tirée sur la table 0-9 du
  livre (l'Épée y figure deux fois, REGLE-07) ; +2 HABILITE tant qu'elle est
  équipée.
- **Sans arme équipée** : -4 HABILITE. Au 260 (combat à mains nues imposé),
  HABILITE de base - 4, sans cumul avec les autres bonus d'arme (REGLE-11).
- **Liens « Sinon »** : condition de valeur `-1` = « si vous ne possédez
  pas » ; les choix « Sinon » sont exclusifs (REGLE-14).
- **Inventaire** : max 2 armes, max 8 (objets + repas confondus), max 50 pièces
  d'or ; objets spéciaux (armure, clés, carte...) illimités.
- **Sac à Dos** (REGLE-12) : Objet Spécial `sac_a_dos`, donné à la création.
  Sans Sac, aucun objet ni repas ne peut être porté (ramassage → 400) ; le
  perdre fait perdre son contenu. Perdu aux 162, 174, 188 (tirage 0-6), 258,
  294 ; ramassable aux 20 et 62.
- **Repas** : consomme 1 Repas à chaque effet `REPAS` d'un chapitre, sinon
  -3 ENDURANCE ; dispensé si discipline Chasse.
- **Discipline Guérison** : +1 ENDURANCE à chaque chapitre traversé sans
  combat (pas sur les sous-sections 2101 et 2102, REGLE-13), ni sur un
  chapitre qui a un effet `ENDURANCE` négatif, même évité (REGLE-18).
- **Discipline Puissance Psychique** : +2 HABILITE en combat, sauf contre un
  ennemi qui y résiste.
- **Effets de chapitre** gérés : `ENDURANCE`, `HABILITE` (temporaire, reset à
  chaque changement de chapitre, sauf condition `PERMANENT` qui modifie la
  valeur de base), `VOL` (la valeur code la portée : 1 = un objet au choix,
  2 = toutes les armes, 8 = contenu du Sac, 9 = le Sac à Dos, 10 = armes +
  contenu du Sac, 11 = Sac à Dos + armes), `REPAS`, `ECHANGE`.
- **Combat** : système propre au projet (tables `TABLE_DEGATS_INFLIGES` /
  `TABLE_DEGATS_SUBIS` rééquilibrées, actions ATTAQUE/DEFENSE/OBJET/FUITE) —
  **différent du système officiel du livre** (qui résout chaque round avec un
  seul tirage sur une table unique, sans choix tactique). Choix assumé
  (décision D-12 du cahier des charges). Voir
  [Limitations connues](#limitations-connues).
- **Quitter un combat** (décision D-11) :
  - `fuite = N` : bouton FUITE actif après N assauts ; `0` = dès la 1re
    action (191, 220, 339), sans pouvoir éviter le combat ;
  - `evitement` : partir sans combattre tant qu'aucun assaut n'est joué
    (112 → 248, 180 → 22) ; une fois engagé, FUITE dès la 1re action ;
  - `evitement = 1` : le lien reste aussi proposé après une victoire
    (112 → 248, DATA-18).
- **Mort** : il faut posséder une Pièce Premium (`coin`, objet spécial) pour
  continuer, que ce soit après une défaite en combat ou une perte
  d'ENDURANCE hors combat. En MVP1 elle n'est **pas consommée** (999 au
  départ).

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
| 409 | Conflit (username/email déjà utilisé, y compris deux inscriptions simultanées refusées par la base), inventaire plein ou action simultanée sur un personnage |
| 500 | Erreur interne : `"message": "Une erreur est survenue (reference xxxx)"`, sans aucun détail interne. La référence est l'identifiant de l'événement Sentry (ou un identifiant aléatoire si Sentry est désactivé) et figure dans les logs (`Erreur interne, reference xxxx`) avec la pile d'appels (SEC-08) |

Les erreurs de Spring MVC (route inconnue 404, méthode non autorisée 405,
JSON illisible 400) gardent leur code et suivent ce même format.

## Tests

```bash
./mvnw test     # tests unitaires et Spring sur H2 : rapides, sans Docker
./mvnw verify   # + tests d'integration *IT sur un vrai PostgreSQL (Docker doit tourner)
```

Les classes `*IT` (package `integration`, QUAL-01) démarrent un PostgreSQL 16
avec Testcontainers : le schéma est créé par les vraies migrations Liquibase,
Hibernate le valide (`ddl-auto=validate`) et le catalogue du livre est chargé,
comme sur Render. La CI (`.github/workflows/sonar.yml`) lance `mvn verify` :
un test en échec fait échouer le contrôle de la PR.

Tests présents notamment sur : authentification (JWT, filtre, service
utilisateur), `PersonnageService`, `InventaireService`, `ObjetService`,
`EffetChapitreService`, `ConditionService`, `ChapitreMapper`, le combat
(`CombatService`, `CombatController`, `TableCombatService`,
`CombatAMainsNuesTest`), `RefreshTokenService`, les emails et le chargement
des données (`GameDataLoaderTest`).

Les tests du package `data/` (`CoherenceLivreTest`,
`StatsEnnemisConformesAuLivreTest`, `LiensDeCombatTest`) vérifient que les
JSON restent fidèles au livre : à garder verts.

> Ne pas lancer `./mvnw test` pendant que `spring-boot:run` tourne (DevTools) :
> l'app répond ensuite 400 sur `/objets`, il faut la redémarrer.

## Structure du projet

```
backend/src/main/java/com/loupsolitaire/backend/
├── config/          # Sécurité, JWT, CORS, chargement des données de jeu (GameDataLoader)
├── controller/       # Auth, Personnage, Combat, Discipline, Objet, Version, Diagnostic
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
├── db/changelog/      # Migrations Liquibase (db.changelog-master.yaml + changes/NNN-*.yaml|sql)
├── static/legal/      # CGU et confidentialité
└── data/
    ├── chapitre.json     # Les 350 chapitres du Livre 1 (texte, liens, effets, ennemis, objets)
    ├── discipline.json   # Les 10 disciplines Kaï
    ├── ennemi.json        # Le bestiaire (25 ennemis)
    └── objet.json         # Le catalogue d'objets (armes, objets spéciaux, consommables, bourse)
```

## Limitations connues

- **Système de combat non fidèle au livre** : le livre résout chaque round
  avec un seul tirage sur une table unique (dégâts des deux camps
  simultanément), sans décision tactique. Ce projet ajoute des actions
  ATTAQUE/DEFENSE/OBJET et des tables rééquilibrées maison. Choix assumé :
  REGLE-01 et REGLE-02 ne sont pas retenus (décision D-12).
- **Fuite** : comme dans le livre, l'ennemi porte un dernier coup pendant la
  fuite (seul Loup Solitaire subit des dégâts). La fuite réussit sauf si ce
  coup le tue (REGLE-03).
- **Chapitre 350 (fin du tome) sans lien de sortie** : son unique lien
  (`351`) est un marqueur de fin de partie filtré au chargement des
  données (`GameDataLoader.PAGES_FIN_DE_JEU`). Le frontend affiche un écran
  de victoire ; pas encore de passage au tome 2.
- **Pièce Premium (`coin`) illimitée** : chaque personnage démarre avec 999
  exemplaires et ils ne sont jamais consommés (MVP1). En pratique, la mort
  (combat ou hors combat) n'a donc actuellement aucune conséquence
  définitive. Un vrai système d'achat/limite est prévu pour une itération
  future (MVP2, REGLE-15 et QUAL-04 dans `../specs`).