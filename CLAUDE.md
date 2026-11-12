# Loup Solitaire v2 — règles du projet

Adaptation numérique du livre-jeu *Loup Solitaire, Tome 1 : Les Maîtres des Ténèbres*.
Monorepo : `backend/` (API Spring Boot) + `frontend/` (Ionic/Angular + Capacitor).
Détails complets : `backend/README.md` et `frontend/README.md`.

## Langue
- Tout est en **français** : code métier (classes, méthodes, variables), commentaires, messages d'erreur, commits.
- Commentaires Java sans accents (style existant) ; textes UI avec accents dans `fr.json`.
- Exceptions techniques gardées en anglais quand le framework l'impose (`AuthRequest`, `RefreshToken`...).

## Stack
- **Backend** : Java 21, Spring Boot, Spring Security + JWT (access 15 min + refresh token opaque haché SHA-256, rotation), JPA/Hibernate, PostgreSQL, Liquibase, Lombok, Sentry, Maven (`./mvnw`).
- **Frontend** : Angular 21 standalone + zoneless + signals, Ionic 9, Capacitor 8, ngx-translate, Vitest, Prettier (`printWidth 100`, `singleQuote`).

## Commandes
```bash
# backend (depuis backend/) — nécessite backend/.env avec DB_PASSWORD et JWT_SECRET
./mvnw spring-boot:run
./mvnw test      # unitaires + Spring sur H2
./mvnw verify    # + tests *IT sur PostgreSQL via Testcontainers (Docker requis) — c'est ce que lance la CI

# frontend (depuis frontend/)
npm install --legacy-peer-deps   # obligatoire (peer deps Ionic/Capacitor)
npm start                        # http://localhost:4200
npm test
npm run build && npx cap sync    # après un changement web, pour Android/iOS
```

## Règles backend
- **Architecture en couches** : `controller` → `service` → `repository`. Les contrôleurs ne contiennent pas de logique.
- `PartieService` est le **point d'entrée unique des actions de jeu** (utilisé par `PersonnageController` et `CombatController`) : il charge le personnage et vérifie qu'il appartient à l'utilisateur connecté. Ne pas contourner.
- **Le serveur fait autorité** sur tous les tirages, dégâts, conditions et effets. Le front n'envoie que des intentions (action, id d'objet, chapitre cible) ; tout est revalidé côté serveur.
- Entités chargées dans la transaction = modifications sauvegardées automatiquement ; `save()` seulement pour les nouvelles entités.
- DTO : `request/` (entrée, validée par `@Valid`), `response/` (sortie, construite par `service/mapper/`), `dto/` = uniquement la lecture des JSON du livre. Ne jamais exposer une entité JPA.
- Erreurs : exceptions de `exception/` → `GlobalExceptionHandler` → `ErrorResponse` JSON unique. Règle métier violée = `IllegalArgumentException` (400) ; 403 accès refusé ; 404 introuvable ; 409 conflit / inventaire plein.
- **Schéma de base = Liquibase uniquement** (`db/changelog/changes/NNN-nom.{yaml,sql}`, ajouté dans `db.changelog-master.yaml`, avec `rollback`, auteur `siwertzm`). Hibernate est en `ddl-auto=validate` : toute modif d'entité demande une migration. Ne jamais modifier une migration déjà fusionnée.
- **Données du livre** : `src/main/resources/data/*.json` font foi, chargées par `GameDataLoader` (échec au démarrage si une donnée est incohérente). DATA-00 : au démarrage, si l'empreinte SHA-256 des JSON a changé, tout le catalogue est rechargé dans une transaction ; **corriger le livre = modifier le JSON, sans migration de données**. Une entrée retirée d'un JSON reste en base (un joueur peut la référencer). Les tests `data/` (`CoherenceLivreTest`, `StatsEnnemisConformesAuLivreTest`, `LiensDeCombatTest`) vérifient la fidélité au livre : les garder verts.
- Sécurité : `JWT_SECRET` et `DB_PASSWORD` n'ont **aucune valeur par défaut** (démarrage refusé sinon). Ne jamais committer `.env` ni de secret. Toute route est protégée sauf `/auth/**`, `/actuator/health` et les pages légales.
- Le hasard du jeu passe par le bean `Random` de `RandomConfig` (injectable/mockable en test), jamais `new Random()`. Les jetons de sécurité utilisent `SecureRandom` (`util/Tokens`).

## Règles du jeu implémentées (à respecter)
- Création : HABILETÉ = 10 + d10(0-9), ENDURANCE = 20 + d10(0-9), exactement 5 disciplines Kaï sur 10.
- Inventaire : max 2 armes, max 8 objets + repas cumulés, max 50 pièces d'or, objets spéciaux illimités.
- HABILETÉ : -4 sans arme, +2 avec l'arme de Maîtrise des Armes ; HABILETÉ temporaire remise à zéro à chaque changement de chapitre (sauf condition `PERMANENT`).
- Repas : 1 repas consommé par effet `REPAS`, sinon -3 ENDURANCE ; Chasse en dispense. Guérison : +1 ENDURANCE par chapitre sans combat.
- Combat : système **maison** (ATTAQUE / DEFENSE / OBJET / FUITE, tables rééquilibrées), volontairement différent du livre. Fuite : l'ennemi porte un dernier coup.
- Mort : Pièce Premium (`coin`), 999 au départ, **pas consommée en MVP1** (ne pas changer sans demande).
- Tirage de la Table de Hasard figé tant que le personnage reste sur le chapitre.

## Règles frontend
- Composants standalone, signals (`signal`, `computed`, `inject()`), pages lazy-loadées dans `app.routes.ts`, `authGuard` sur toutes les routes de jeu.
- **Aucune chaîne en dur** : tous les textes dans `public/assets/i18n/fr.json`, un namespace par page/composant, interpolation `translate:{ param }`.
- Appels API uniquement via les services de `core/services/` ; modèles TS de `core/models/` alignés sur les `request`/`response` Java → modifier les deux ensemble.
- Tokens stockés via `@capacitor/preferences` (`TokenStorageService`), refresh automatique dans `auth.interceptor.ts`.
- Toute nouvelle logique frontend non triviale (service, intercepteur, guard) vient avec son test Vitest (`*.spec.ts` à côté du fichier) ; la CI lance `npm test`.

## Workflow Git
- Jamais de commit direct sur `main` : une branche par ticket puis une PR. Nommage `feature-<CODE>` / `fix-<CODE>` avec les codes de ticket existants (`SEC-xx`, `OPS-xx`, `RGPD-xx`, `REGLE-xx`, `QUAL-xx`).
- La CI (`sonar.yml` : `mvn verify` + SonarCloud) est un contrôle obligatoire de PR ; corriger les alertes Sonar.
- Toute nouvelle logique métier vient avec son test (dans le même package sous `src/test`).
