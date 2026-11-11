# Specs — corrections Loup Solitaire (Tome 1)

Une tâche = un dossier `lot-N/<ID>-<nom>/spec.md`, avec le problème, ce qu'il faut faire, les critères d'acceptation, les tests et la branche à créer (`fix-<ID>` ou `feature-<ID>`, voir `CLAUDE.md`).

- **Source** : `source/` contient le cahier des charges d'origine (audit du 25/09/2026) et les trois listes de lots. Le détail des constats, les écarts VO/VF (section 5) et les décisions D-01 à D-10 (section 7) y restent.
- **Ajouts** : SEC-09, QUAL-06 et OPS-08 viennent de l'audit Claude Code du 11/11/2026. SEC-06 et OPS-05 existaient déjà et ont été complétés.
- **Second audit du 11/11/2026** (textes, i18n, règles `CLAUDE.md`) : SEC-10, QUAL-07, QUAL-08, REGLE-16 et QUAL-09 ajoutés ; QUAL-03 complété.
- Seules les tâches **à faire** ont un dossier. Quand une tâche est finie, passer son statut à « Fait » dans son `spec.md` et dans le tableau ci-dessous.

## Tâches à faire

### Lot 1 — avant d'élargir le test TestFlight

| ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- |
| [SEC-09](lot-1/SEC-09-refresh-concurrent/spec.md) | Critique | S | Déconnexion forcée quand plusieurs requêtes renouvellent la session en même temps |
| [OPS-06](lot-1/OPS-06-sauvegardes-restauration/spec.md) | Haute | M | Sauvegardes, restauration et retour arrière |
| [OPS-08](lot-1/OPS-08-mvnw-executable/spec.md) | Basse | S | `backend/mvnw` sans droit d'exécution dans git |
| [SEC-10](lot-1/SEC-10-routes-auth-me/spec.md) | Haute | S | `/auth/me` répond 500 au lieu de 401 quand le jeton a expiré |
| [QUAL-07](lot-1/QUAL-07-textes-francais/spec.md) | Moyenne | S | fr.json : vouvoiement, HABILETÉ, fautes et cohérence |

### Lot 2 — premier mois

| ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- |
| [SEC-06](lot-2/SEC-06-changement-email/spec.md) | Moyenne | M | Mot de passe exigé pour changer d'email ; ancien email actif jusqu'à confirmation |
| [OPS-05](lot-2/OPS-05-purge-tokens/spec.md) | Moyenne | S | Purge quotidienne des tokens expirés et des comptes non vérifiés |
| [QUAL-06](lot-2/QUAL-06-tests-frontend-ci/spec.md) | Moyenne | S | Tests frontend et lancement dans la CI |
| [DATA-00](lot-2/DATA-00-catalogue-versionne/spec.md) | Moyenne | M | Catalogue versionné (à faire avant les autres DATA) |
| [DATA-16](lot-2/DATA-16-repas-184/spec.md) | Basse | S | 184 : 1 Repas obligatoire + 3 facultatifs |
| [OPS-01](lot-2/OPS-01-memoire-jvm/spec.md) | Moyenne | S | Réglage mémoire de la JVM et sondes |
| [OPS-03](lot-2/OPS-03-chargement-livre-transactionnel/spec.md) | Moyenne | S | Chargement du livre transactionnel + contrôle d'intégrité |
| [SEC-05](lot-2/SEC-05-anti-enumeration/spec.md) | Moyenne | S | Réponses identiques à l'inscription |
| [SEC-07](lot-2/SEC-07-h2-et-cve/spec.md) | Basse | S | H2 hors du jar ; contrôle des CVE |
| [SEC-08](lot-2/SEC-08-erreurs-409-500/spec.md) | Basse | S | 409 sur violation d'unicité, 500 journalisé |
| [RGPD-02](lot-2/RGPD-02-export-donnees/spec.md) | Moyenne | S | Export des données en JSON |
| [RGPD-03](lot-2/RGPD-03-conservation-et-logs/spec.md) | Moyenne | S | Conservation ; emails masqués dans les logs |
| [REGLE-04](lot-2/REGLE-04-aide-combat-regle-maison/spec.md) | Basse | S | Aide du combat : DEFENSE et OBJET |
| [REGLE-06](lot-2/REGLE-06-pas-de-jet-en-combat/spec.md) | Moyenne | S | Pas de jet d'objet en combat |
| [REGLE-07](lot-2/REGLE-07-table-maitrise-armes/spec.md) | Moyenne | S | Table 0-9 de la Maîtrise des Armes |
| [REGLE-09](lot-2/REGLE-09-vol-144-sac-puis-arme/spec.md) | Moyenne | S | Vol du 144 : Sac d'abord |
| [REGLE-11](lot-2/REGLE-11-mains-nues-260/spec.md) + [DATA-13](lot-2/DATA-13-marqueur-sans-arme-260/spec.md) | Moyenne | S | 260 : mains nues sans cumul |
| [REGLE-12](lot-2/REGLE-12-sac-a-dos/spec.md) | Moyenne | M | Modéliser le Sac à Dos |
| [REGLE-13](lot-2/REGLE-13-guerison-sous-sections/spec.md) | Basse | S | Pas de Guérison sur 2101/2102 |
| [REGLE-14](lot-2/REGLE-14-choix-sinon-exclusifs/spec.md) | Basse | S | Choix « Sinon » exclusifs |
| [REGLE-15](lot-2/REGLE-15-coin-decompte/spec.md) | Moyenne | M | Coin décompté (met `CLAUDE.md` à jour) |
| [DATA-06](lot-2/DATA-06-perte-equipement-205/spec.md) + [DATA-07](lot-2/DATA-07-pas-de-vol-181/spec.md) | Moyenne | S | Perte d'équipement au 205, pas au 181 |
| [DATA-14](lot-2/DATA-14-pierres-137/spec.md) | Moyenne | S | 137 : Pierres = 1 objet de Sac |
| [QUAL-02](lot-2/QUAL-02-version-api/spec.md) | Moyenne | M | Version minimale de l'app, /api/v1, OpenAPI |
| [QUAL-08](lot-2/QUAL-08-textes-en-dur/spec.md) | Moyenne | S | Textes en dur ; messages du serveur affichés tels quels |
| [REGLE-16](lot-2/REGLE-16-echange-unique/spec.md) | Basse | S | Échange du 307 répétable via l'API |

### Lot 3 — plus tard (MVP2, optimisation)

| ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- |
| [REGLE-10](lot-3/REGLE-10-assaut-min/spec.md) + [DATA-12](lot-3/DATA-12-vordak-283/spec.md) | Basse | M | ASSAUT_MIN, Vordak du 283 |
| [OPS-07](lot-3/OPS-07-requetes-sql/spec.md) | Basse | M | Moins de requêtes SQL par action |
| [QUAL-03](lot-3/QUAL-03-nommage/spec.md) | Basse | S | Convention de nommage |
| [QUAL-04](lot-3/QUAL-04-portefeuille-coins/spec.md) | Haute (MVP2) | L | Portefeuille de coins |
| [QUAL-05](lot-3/QUAL-05-multi-tomes/spec.md) | Moyenne (MVP2) | L | Modèle multi-tomes |
| [QUAL-09](lot-3/QUAL-09-choix-langue/spec.md) | Moyenne | M | Choix de la langue ; en.json corrigé |

## Statuts vérifiés le 11/11/2026

Les statuts des listes de `source/` ont été contrôlés contre le code et `chapitre.json`.

**Corrigé :**
- **DATA-16** était marqué « Fait » : le 184 a toujours `repas` × 4 obligatoires et aucune migration ne le change. Remis à faire.

**À confirmer (marqués « Déjà OK », mais les données ne suivent pas le cahier) :**
- **DATA-05** : le lien 112 → 248 est toujours `fuite = 0`. Le code le traite exprès comme « combat évitable » (`PersonnageService.avancerVersChapitre`, chapitres 112 et 180). Si c'est un choix, l'écrire comme décision D-11 ; sinon, créer la spec.
- **DATA-11** : le lien 339 → 7 est toujours `fuite = 1`, le cahier demande `fuite = 0`. Même question.
- **REGLE-01 / REGLE-02** : le combat utilise toujours sa table maison et deux tirages par assaut. C'est cohérent avec `CLAUDE.md` (« système maison, volontairement différent du livre ») : à écrire comme décision D-11, pour que le cahier ne dise plus « Critique ».

**Vérifié « Fait » :** SEC-01 à SEC-04, OPS-02, OPS-04, REGLE-03, REGLE-05 (consommation bloquée pendant un combat en cours), REGLE-08, DATA-01 à DATA-04, DATA-08 à DATA-10, RGPD-01, RGPD-04, QUAL-01 (`./mvnw verify` vert avec Testcontainers, 600 tests).

## Ordre conseillé

1. **SEC-09** tout de suite : c'est le seul problème que les testeurs TestFlight vont voir. **SEC-10** dans la foulée (même symptôme côté Profil après 15 minutes).
2. **OPS-08** dans la même petite PR que le `.gitignore` (graphify-out, Zone.Identifier).
3. **OPS-06** avant d'ouvrir l'app au public.
4. Lot 2 : **DATA-00** d'abord (les corrections DATA suivantes n'auront plus besoin de migration), puis SEC-06, OPS-05 et QUAL-06.
