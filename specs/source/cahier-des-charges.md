# Cahier des charges — corrections Loup Solitaire (Tome 1)

Sep 25, 2026 · @marius

Audit en lecture seule de siwertzm/loup-solitaire-v2 (main, commit a96e67d), comparé au livre Gallimard (VF) et à l'édition Project Aon (VO). Maven Central est bloqué dans l'environnement d'audit : les tests n'ont pas été exécutés, les constats reposent sur la lecture du code et sur des scripts Python appliqués aux JSON et aux textes.

## 1. Synthèse

**Verdict : pas prêt pour la production grand public.** Le moteur de navigation et les données sont globalement fidèles (577 liens, 44 plages de hasard et toutes les pertes d'ENDURANCE vérifiés par script), mais cinq points bloquent un lancement public.

1. **Triche à la création (SEC-01)** : les tirages d'HABILETÉ et d'ENDURANCE sont faits par le client (`Math.random()`) et acceptés sans borne par le serveur. Un appel forgé crée un personnage à 1 010 d'HABILETÉ.
2. **Combat non conforme au livre (REGLE-01 à 03)** : 116 des 130 cellules « LS » de la Table des Coups Portés diffèrent de l'officielle, et le joueur et l'ennemi utilisent deux tirages séparés. Par simulation, un Gourgaz passe de 50 % à 57 % de victoires, un Vordak de 88 % à 97 %.
3. **Données d'ennemis fausses (DATA-01 à 04)** : le Serpent Ailé (133) est à 12/10 au lieu de 16/18 ; trois ennemis réutilisés (220, 229, 340) gardent les stats d'une autre section.
4. **Aucune protection contre les abus (SEC-02)** : ni limitation de débit, ni envoi d'emails asynchrone. Le code de réinitialisation à 6 chiffres est attaquable sans limite, et une panne SMTP peut épuiser le pool de 10 connexions.
5. **Secret JWT par défaut (SEC-03)** : si `JWT_SECRET` manque sur Render, l'application démarre avec une clé publiée dans le dépôt, et n'importe qui peut forger un jeton.

Les trois points volontairement écartés deviennent nécessaires : limitation de débit et envoi asynchrone avant le lancement, purge des tokens dans le premier mois (SEC-02, OPS-02, OPS-05).

## 2. Corrections de données

Le reste des données est conforme : 577 liens (hors 201), 44 plages de hasard, 18 effets d'ENDURANCE, 7 repas, 17 morts narratives et 25 combats sur 29 correspondent au texte. Comme `GameDataLoader` ne recharge rien si la base est remplie (`GameDataLoader.java:86,110,160,222`), chaque ligne « oui » exige un changeset Liquibase en plus de la modification du JSON. Les deux doivent partir dans le même commit, sinon une base neuve et la base de production divergent.

| ID | Section | Élément | Valeur actuelle | Valeur attendue | Source (FR/EN) | Fichier JSON | Migration SQL |
| --- | --- | --- | --- | --- | --- | --- | --- |
| DATA-01 | 133 | Serpent Ailé (`serpent_aile`) | HAB 12, END 10 | HAB 16, END 18 | FR « SERPENT AILÉ HABILETÉ : 16 ENDURANCE : 18 » ; EN « COMBAT SKILL 16 ENDURANCE 18 » | ennemi.json | Oui |
| DATA-02 | 229 | Kraan (réutilise `kraan` du 17) | END 24 | END 25, nouvel ennemi `kraan_229` | FR « KRAAN HABILETÉ: 16 ENDURANCE: 25 » ; EN idem | ennemi.json + chapitre.json | Oui |
| DATA-03 | 220 | Garde du corps (réutilise `garde_corps` du 191) | END 21 | END 20, nouvel ennemi `garde_corps_220` | FR « GARDE DU CORPS HABILETÉ : 11 ENDURANCE : 20 » ; EN idem | ennemi.json + chapitre.json | Oui |
| DATA-04 | 340 | Glok + Loup Maudit (réutilise le 72) | HAB 15 | HAB 14, nouvel ennemi `glok_loup_maudit_340` | FR « HABILETE. 14 ENDURANCE : 24 » ; EN « COMBAT SKILL 14 » | ennemi.json + chapitre.json | Oui |
| DATA-05 | 112 | Lien vers 248 | `fuite=0` : fuite possible sans combattre | `victoire=1` | FR « Si vous êtes vainqueur, vous pouvez explorer… Mais vous pouvez également quitter les lieux » ; EN idem | chapitre.json | Oui |
| DATA-06 | 205 | Perte de l'équipement | Aucun effet | `vol` valeur 10 (armes + sac), nom « perte » | FR « Leur chef ramasse votre Équipement » ; EN « You must now erase all Weapons and Backpack Items » | chapitre.json | Oui |
| DATA-07 | 181 | Perte du sac | `vol` valeur 8, toujours appliqué | Aucun effet (la perte passe au 205) | EN, errata : phrase retirée « car on peut atteindre cette section sans avoir perdu son équipement » (arrivée par 22) | chapitre.json | Oui (retenu, D-03) |
| DATA-08 | 201 | Lien « est » | 215 (VF) | 15 (VO) | EN « head east, turn to 15 » + errata ; le 15 renvoie au 201 vers l'ouest | chapitre.json | Oui (retenu, D-02) |
| DATA-09 | 291 | Lances ramassables | `lance` × 2 | `lance` × 1 (le livre impose « le Poignard ou l'une des Lances ») | FR « prendre au choix le Poignard ou l'une des Lances » ; EN idem | chapitre.json | Oui |
| DATA-10 | 62 | 3 Repas des brigands | Obligatoires | Optionnels | FR règle « Chaque fois que vous déciderez d'emporter l'un de ces objets » ; rien n'impose de les prendre | chapitre.json | Oui |
| DATA-11 | 339 | Seuil de fuite vers 7 | `fuite=1` | `fuite=0` | FR « prendre la fuite à tout moment » ; EN « at any stage of the fight » | chapitre.json | Oui |
| DATA-12 | 283 | Malus du Vordak (−2 sans Bouclier) | S'applique dès le 1er assaut | À partir du 2e assaut seulement ; nécessite REGLE-10 | FR « Dès le deuxième assaut… réduire de 2 points » ; EN « second and subsequent rounds » | chapitre.json | Oui (après REGLE-10) |
| DATA-13 | 260 | Malus « mains nues » | Effet `habilite -4` | Marqueur « combat sans arme » ; nécessite REGLE-11 (sinon −8 si le joueur est déjà désarmé) | FR « votre total d'HABILETÉ se trouvera diminué de 4 points » | chapitre.json | Oui (après REGLE-11) |
| DATA-14 | 137 | 20 Pierres Précieuses | Catégorie BOURSE, compte dans les 50 pièces | 1 objet de Sac à Dos, nouvel objet pierres\_tombeau (D-04) | FR « Elles prennent place dans votre bourse » ; EN « as a single Backpack Item » | objet.json + chapitre.json | Oui |
| DATA-15 | 164 | Essence d'Alether | Catégorie OBJET (Sac à Dos) | Aucun changement, reste dans le Sac à Dos (D-05) | FR « Inscrivez cette Potion dans la case Objets Spéciaux » ; EN sans précision | objet.json | Non |
| DATA-16 | 184 | 4 Repas trouvés dans la roulotte | repas × 4, obligatoires | repas × 1 obligatoire + repas × 3 facultatifs ; ordre inchangé, le repas imposé peut utiliser l'obligatoire (D-10) | FR « Si vous souhaitez conserver l'une ou l'autre de ces trouvailles » puis « il vous faut prendre un Repas » ; EN idem | chapitre.json (+ code, voir D-10) | Oui |

Exemple de changeset (à compléter pour chaque ligne) :

```sql
-- 011-corrections-donnees-livre.sql
UPDATE ennemi SET habilite = 16, endurance = 18 WHERE id = 'serpent_aile';
INSERT INTO ennemi (id, nom, description, habilite, endurance)
SELECT 'kraan_229', nom, description, 16, 25 FROM ennemi WHERE id = 'kraan';
UPDATE chapitre_ennemi SET ennemi_id = 'kraan_229' WHERE chapitre_id = 229;
UPDATE cond SET type = 'VICTOIRE', valeur = '1'
 WHERE type = 'FUITE'
   AND lien_id IN (SELECT id FROM lien WHERE chapitre_id = 112 AND chapitre_cible_id = 248);
UPDATE objet_chap SET valeur = 1 WHERE chapitre_id = 291 AND objet_id = 'lance';
INSERT INTO effet (id, type, valeur, nom, chapitre_id)
VALUES (gen_random_uuid(), 'VOL', 10, 'perte', 205);
```

Pour éviter une migration SQL à chaque correction, DATA-00 propose de versionner le catalogue : une empreinte SHA-256 des quatre JSON est stockée en base, et si elle change, `GameDataLoader` recharge dans une transaction les tables enfants (lien, cond, effet, objet\_chap, chapitre\_ennemi) et met à jour ennemi, objet et chapitre. C'est possible car les données des joueurs ne référencent que des identifiants stables (numéro de chapitre, identifiants d'objet et d'ennemi). Effort M. La vidange du cache de second niveau se fait au redémarrage.

## 3. Corrections de règles et de code

Déjà conforme : HABILETÉ 10 + tirage et ENDURANCE 20 + tirage, 5 disciplines distinctes, équipement de départ et table d'objet 0-9 (identique à la VF), bonus du Casque (+2) et de la Cotte (+4), Puissance Psychique et immunités, −4 sans arme, limites 2 armes / 8 objets / 50 couronnes, repas −3 et Chasse, tirage figé par section, conditions VICTOIRE, ASSAUT\_MAX, ASSAUT\_ECHEC, ENDURANCE\_PERDUE et sous-sections 2101/2102. Chemins : `service/` = `backend/src/main/java/com/loupsolitaire/backend/service/`.

| ID | Sévérité | Constat (fichier:ligne) | Risque concret | Correction attendue | Critère d'acceptation | Effort |
| --- | --- | --- | --- | --- | --- | --- |
| REGLE-01 | Critique | La colonne « LS » de la Table des Coups Portés est une table « équilibrée » du prototype : 116 cellules sur 130 diffèrent de crtneg.png et crtpos.png (`TableCombatService.java:37-51`). Exemple, quotient −11 : officiel \[0, K, K, 8, 8, 7, 6, 5, 4, 3\] pour les tirages 0 à 9, code \[K, 0, 3, 4, 5, 6, 7, 8, 9, K\]. La colonne « E » est exacte (130/130). | Le tirage 0 tue Loup Solitaire à −11 alors que le livre en fait le meilleur tirage ; la difficulté de chaque combat diffère du livre. | Remplacer par une seule table `CRT[13][10]` de couples (E, LS) transcrite des images, `K` = mort immédiate. | Test JUnit comparant les 130 couples à une fixture transcrite des images. | S |
| REGLE-02 | Haute | Deux tirages distincts par assaut, l'un pour l'ennemi, l'autre pour le joueur (`CombatService.java:176`, `:195`), et aucune perte pour le joueur quand l'ennemi meurt (`:185-190`). Le livre : un seul tirage donne les deux pertes (VF règles, point 4 : « les points d'ENDURANCE que l'ennemi et le Loup Solitaire auront chacun perdu lors de cet assaut »). | Combats plus faciles qu'au livre. Par simulation (200 000 combats) : Gourgaz 20/30 contre LS 15/25 = 50,5 % au livre, 57 % dans l'app ; Vordak avec −2 = 88 % contre 97 %. | `r = tirer(); (e, l) = CRT[col][r]; ennemi -= e; joueur -= l;` puis tester la mort du joueur avant de passer à l'ennemi suivant. | Avec un `Random` bouchonné, un assaut qui tue l'ennemi retire aussi `l` au joueur ; un seul appel à `tirerChiffre()` par assaut. | S |
| REGLE-03 | Haute | La fuite réussit sans riposte (`CombatService.java:259-268`). Le livre : l'assaut de fuite est joué, seul Loup Solitaire perd des points (VF « Possibilités de fuite » ; EN « Evasion of Combat »). | La fuite est gratuite, et le 43 annonce pourtant « au risque d'être blessé en fuyant ». | Pour FUITE : tirer, appliquer `l` au joueur, ignorer `e`, puis passer le statut à FUITE ou DEFAITE. | Un test FUITE montre une perte égale à `CRT[col][r].l` et une ENDURANCE ennemie inchangée. | S |
| REGLE-04 | Basse (écart assumé, D-01) | Les actions DEFENSE (`CombatService.java:203-226`, tables `TableCombatService.java:56-57`) et OBJET (`:228-257`) n'existent pas dans le livre. DEFENSE avec le tirage 0 bloque 100 % des dégâts et donne +5 HABILETÉ à l'attaque suivante ; elle compte comme un assaut pour les seuils des sections 231 et 339. | Écart non documenté, qui fausse les seuils « tuer en 4 assauts ». | Garder les deux actions (D-01) et les décrire comme règle maison dans l'aide du combat. | La règle maison est affichée dans l'aide du combat. | M |
| REGLE-05 | Haute | `POST /personnages/{id}/objets/{objetId}/consommer` ne vérifie pas le combat (`PartieService.java:257-263`), et l'action OBJET permet de boire en plein assaut. Le livre : la Potion se boit « à l'issue d'un combat » (VF Équipement, 6) ; l'Alether « au début d'un combat » (164). | Soin gratuit en plein combat, sans riposte. | Refuser la consommation d'une potion de soin si un combat est EN\_COURS sur le chapitre ; n'autoriser l'Alether qu'avant le premier assaut. | Test : potion pendant un combat EN\_COURS → 400. | S |
| REGLE-06 | Moyenne | `DELETE /objets/{objetId}` reste possible pendant un combat (`PartieService.java:249-253`). Le livre l'interdit (VF : « il vous est interdit de le faire lorsque vous êtes engagé dans un combat »). | Écart de règle ; faible gain pour le joueur. | Refuser si un combat est EN\_COURS. | Test : DELETE pendant un combat → 400. | S |
| REGLE-07 | Moyenne | L'arme de Maîtrise est tirée uniformément parmi les 9 armes du catalogue (`PersonnageService.java:156-159`). Le livre : table 0-9 où l'Épée sort sur 5 et sur 7. | Épée 11 % au lieu de 20 %. | Table fixe 0 poignard, 1 lance, 2 masse, 3 sabre, 4 marteau, 5 épée, 6 hache, 7 épée, 8 bâton, 9 glaive. | Test paramétré sur les 10 tirages. | S |
| REGLE-08 | Moyenne | L'ordre des ennemis multiples n'est pas garanti : `Chapitre.ennemis` est une `List` `@ManyToMany` sans `@OrderColumn` (`model/Chapitre.java:50-57`), et `chapitre_ennemi` n'a pas de colonne d'ordre (`001-baseline.sql:24-27`). | Aux 112, 180, 253, 260 et 336, PostgreSQL peut renvoyer les ennemis dans un autre ordre ; par exemple le Chef (15/22) avant ou après les soldats. | Ajouter une colonne `ordre` avec migration de remplissage et `@OrderColumn(name = "ordre")`. | Test d'intégration : le combat du 253 aligne `loup_maudit_1` à `_4` dans l'ordre du JSON. | S |
| REGLE-09 | Moyenne | Au vol du 144, la portée TOUT accepte une arme même si le Sac contient des objets (`EffetChapitreService.java:248-255`, `:279-281`). Le livre : un objet du Sac, et une arme seulement si le Sac est vide. | Le joueur choisit de perdre une arme en trop plutôt qu'un objet utile. | Nouvelle portée `SAC_PUIS_ARME`. | Test : Sac non vide, choix d'une arme → 400. | S |
| REGLE-10 | Basse | Seul un effet dont l'unique condition est ASSAUT\_MAX est géré par assaut (`CombatService.java:354-362`) ; « à partir du 2e assaut » (283) n'est pas exprimable. | Au 283, sans Bouclier Psychique, le 1er assaut vaut +0 au lieu de +2. | Nouveau type `ASSAUT_MIN`, combinable avec DISCIPLINE −1 ; mettre à jour la contrainte CHECK `cond_type_check` (`001-baseline.sql:73`). | Test 283 : +2 au 1er assaut, −2 ensuite sans Bouclier, 0 ensuite avec. | M |
| REGLE-11 | Moyenne | Au 260, le malus « mains nues » est un effet −4, alors que `recalculerHabiliteArmes` ajoute déjà −4 sans arme (`InventaireService.java:501-502`). | −8 pour un joueur arrivé sans arme. | Condition « combat sans arme » qui force `habiliteBase − 4` pour ce combat, sans cumul. | Test 260, avec et sans arme : HABILETÉ effective = base − 4 dans les deux cas. | S |
| REGLE-12 | Moyenne (retenu, D-06) | Le Sac à Dos n'est pas modélisé. Aux 174, 188, 258 et 294, le livre fait perdre le Sac lui-même (le 20 en fournit un autre), mais l'app laisse ramasser des objets de Sac juste après. | Écart de règle. | Booléen `possedeSac` sur Personnage ; ramasser un objet de Sac sans Sac → 400 ; objet `sac_a_dos` au 20. | Test : après le 174, ramasser un Repas → 400. | M |
| REGLE-13 | Basse | La Guérison s'applique aux sous-sections 2101 et 2102 (`PersonnageService.java:437-450`). | Jusqu'à +2 ENDURANCE en trop sur la section 21. | Ne pas appliquer la Guérison aux identifiants > 350. | Test 21 → 2101 → 2102 : +1 au total. | S |
| REGLE-14 | Basse | Les choix « Sinon » ne sont pas exclusifs : la valeur −1 est ignorée sur un lien (`ConditionService.java:247-250`, la quantité est toujours ≥ −1). Concerne 9, 52, 105, 128, 162, 173, 242, 272 et 303. | Un joueur qui a la Pierre de Vordak peut quand même choisir la mort du 292. | Interpréter `valeur = -1` comme « ne possède pas », puis ajouter ces conditions dans les JSON (+ migration). | Test 9 avec la Pierre : le lien vers 292 est indisponible. | S |
| REGLE-15 | Moyenne (retenu, D-07) | Le retour par coin et la résurrection rendent toute l'ENDURANCE (`PersonnageService.java:362`, `:427`), et le coin n'est jamais décompté (999 donnés à la création, `:171`). Revenir au 144 réapplique −2 ENDURANCE et un vol à chaque passage. | Mourir soigne ; modèle incompatible avec un coin payant. | Retirer 1 coin à chaque retour ou résurrection dès le MVP1, refuser à 0 coin, stock initial de 999 pendant la bêta ; le portefeuille complet reste QUAL-04. | — | M |

## 4. Exigences de mise en production

Déjà bien fait : contrôle du propriétaire sur chaque action (`PartieService.java:334-345`), verrou optimiste avec réponse 409, refresh tokens hachés en SHA-256 avec rotation, code à 6 chiffres haché en BCrypt, CORS sans credentials, suppression de compte complète (`CompteService.java:143-160`), actuator limité à `health`, conteneur non-root. Top 10 OWASP API : BOLA couvert ; authentification (API2) → SEC-02 et SEC-04 ; consommation de ressources (API4) → SEC-02 ; flux métier (API6) → SEC-01 et REGLE-05 ; mauvaise configuration (API8) → SEC-03 ; inventaire (API9) → QUAL-02 ; les autres catégories ne s'appliquent pas.

| ID | Sévérité | Constat (fichier:ligne) | Risque concret | Correction attendue | Critère d'acceptation | Effort |
| --- | --- | --- | --- | --- | --- | --- |
| SEC-01 | Critique | Les tirages de création viennent du client : `Math.random()` (`frontend/…/creation-personnage.page.ts:91`, `:146-147`), sans `@Min`/`@Max` (`CreerPersonnageRequest.java:18-22`), utilisés tels quels (`PersonnageService.java:100`, `:107`). Autres trous : `nom` sans taille maximale, `disciplines` null → NPE et 500 (`PersonnageController.java:45`). | `hasardHabilite: 1000` → HABILETÉ 1 010 ; un joueur relance aussi jusqu'à obtenir 9/9. | Tirer côté serveur. Deux options : `POST /personnages/tirage` qui fige le tirage en base, ou tirage à la création (le front affiche le résultat). Ajouter `@Size(max = 40)` sur `nom` et `@NotNull` sur `disciplines`. | Un POST avec `hasardHabilite = 1000` est ignoré ou renvoie 400 ; HABILETÉ ∈ \[10, 19\] et ENDURANCE ∈ \[20, 29\] sur 1 000 créations. | S |
| SEC-02 | Critique | Aucune limitation de débit. Chaque `forgot-password` crée un nouveau code et remet le compteur d'essais à zéro (`PasswordResetService.java:47-64`) : 5 essais par code, codes illimités. `login` coûte un BCrypt par appel ; `register` et `resend-verification` envoient des emails sans limite. | Prise de compte par force brute du code (10⁶ combinaisons à 5 essais par email demandé) ; bombardement d'emails qui épuise le quota Brevo et dégrade la délivrabilité ; saturation CPU d'une petite instance Render. **Nécessaire avant le lancement.** | Bucket4j + Caffeine en mémoire (une seule instance) : par IP sur `/auth/**`, par email sur `forgot-password`, `verify-reset-code` et `resend-verification` (par exemple 3 codes par heure et 10 échecs par 24 h, puis blocage). `server.forward-headers-strategy=framework` pour lire l'IP réelle derrière le proxy Render. | Test d'intégration : le 4e `forgot-password` en une heure pour un même email reçoit 429 ; le 11e login en une minute depuis une même IP reçoit 429. | M |
| SEC-03 | Critique | Secrets par défaut : `jwt.secret=${JWT_SECRET:bG91cC1z…}` (`application.properties:123`) et `DB_PASSWORD:postgres` (`:14`). | Une variable oubliée sur Render suffit pour démarrer avec une clé publique : n'importe qui peut forger un JWT. | Aucune valeur par défaut hors profil `dev` ; démarrage refusé si la variable manque en `prod`. | Lancée sans `JWT_SECRET` en profil prod, l'application refuse de démarrer. | S |
| SEC-04 | Haute | La détection de réutilisation d'un refresh token est annulée : `revoquerToutesLesSessions` (`RefreshTokenService.java:52`) est suivi d'un `throw TokenInvalideException` (RuntimeException) dans une méthode `@Transactional` (`:45`) appelée par `AuthService.rafraichir`, lui aussi `@Transactional`. Le rollback efface la révocation. | Un token volé et déjà utilisé déclenche l'alerte, mais les sessions de l'attaquant restent valides. | `@Transactional(noRollbackFor = TokenInvalideException.class)` sur les deux méthodes, ou révocation dans une transaction `REQUIRES_NEW`. | Test d'intégration sur une vraie base (pas de mock) : après une réutilisation, tous les tokens de l'utilisateur ont `revoked = true` en base. | S |
| SEC-05 | Moyenne | Énumération de comptes : `register` répond 409 « Cet email est deja utilise » (`CompteService.java:54`, `:105`), et `forgot-password` n'envoie l'email en synchrone que si le compte existe (différence de temps de réponse). | Liste des emails inscrits récupérable. | Réponse 202 identique dans tous les cas, avec un email « compte déjà existant » à l'adresse ; envoi asynchrone (OPS-02). | Temps de réponse et corps identiques pour un email connu et inconnu. | S |
| SEC-06 | Moyenne | Changer d'email ne demande pas le mot de passe (`CompteService.java:92-118`) et désactive la vérification tout de suite. | Un access token volé (15 min) suffit à détourner le compte ; une faute de frappe bloque la connexion. | Exiger le mot de passe ; garder l'ancien email actif jusqu'à confirmation du nouveau ; prévenir l'ancienne adresse. | Sans mot de passe → 401 ; tant que le lien n'est pas confirmé, la connexion avec l'ancien email fonctionne. | M |
| SEC-07 | Basse | H2 embarqué dans le jar de production (`pom.xml:103-106`, scope `runtime`). Versions : Spring Boot 4.1.1 (qui gère Spring Security, Hibernate, Jackson, le pilote PostgreSQL, Liquibase et Caffeine), JJWT 0.13.0, JaCoCo 0.8.12. Les CVE n'ont pas pu être vérifiées : Maven Central est bloqué ici. | Surface inutile ; vulnérabilités non suivies. | H2 en scope `test` ; Dependabot ou OWASP dependency-check dans la CI. | La CI échoue sur une CVE de sévérité haute. | S |
| SEC-08 | Basse | Pas de gestionnaire pour `DataIntegrityViolationException` (inscriptions simultanées, `nom` de plus de 255 caractères) ni pour `IllegalStateException` (`GlobalExceptionHandler.java`). | 500 sur une erreur du joueur. | Gestionnaire 409 pour les violations d'unicité ; repli générique 500 journalisé avec un identifiant de corrélation. | Deux inscriptions parallèles avec le même email → une 201 et une 409. | S |
| RGPD-01 | Haute (décidé, D-08) | La date de naissance est collectée (`RegisterRequest`, `Utilisateur.java:57`) mais inutilisée, sans contrôle d'âge. Un livre-jeu attire des mineurs. | En France, un mineur de moins de 15 ans ne peut consentir seul pour les services en ligne (point à faire valider par un juriste). | Supprimer le champ de l'API, de l'entité et de la base (migration DROP COLUMN). | GET /auth/me ne renvoie plus de date de naissance et la colonne n'existe plus. | S/M |
| RGPD-02 | Moyenne | Pas d'export des données ; `GET /auth/me` ne couvre pas l'inventaire ni le journal. | Droit d'accès et de portabilité non outillé. | `GET /auth/me/export` en JSON (profil, personnages, inventaires, journaux). | Export complet vérifié par un test. | S |
| RGPD-03 | Moyenne | Aucune durée de conservation : comptes jamais vérifiés et tokens gardés indéfiniment ; email en clair dans les logs d'erreur (`EmailService.java:331-335`, `:623-627`). | Non-conformité à la minimisation. | Purge des comptes non vérifiés après 7 jours (OPS-05) ; email masqué dans les logs (`j***@domaine`). | Job testé ; aucun email complet dans les logs. | S |
| RGPD-04 | Moyenne | Pas de politique de confidentialité ni de CGU côté API ; Brevo est un sous-traitant. Les emails transactionnels ne demandent pas de consentement marketing. | Mentions obligatoires manquantes au lancement. | Politique de confidentialité, registre des traitements, accord de sous-traitance Brevo, lien dans les emails. | Documents publiés et liés depuis l'app. | S |
| OPS-01 | Moyenne | `ENTRYPOINT ["java", "-jar", …]` sans réglage mémoire (`Dockerfile:30`) : la JVM prend 25 % de la RAM par défaut, soit 128 Mo sur 512 Mo. | OutOfMemoryError ou GC permanent sur une petite instance. | `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` ; sondes `management.endpoint.health.probes.enabled=true`. | Test de charge à 50 utilisateurs simultanés sans OOM sur 512 Mo. | S |
| OPS-02 | Haute | Emails envoyés en synchrone dans les transactions (`CompteService.inscrire` → `EmailVerificationService.java:41` ; `PasswordResetService.java:66`), sans délai maximal SMTP (`application.properties:185-191`). | Si Brevo ne répond plus, chaque requête garde une connexion du pool (10 max, `:24`) : 10 inscriptions bloquées suffisent à rendre l'API indisponible. **Nécessaire avant le lancement.** | `mail.smtp.connectiontimeout`, `timeout` et `writetimeout` à 5000 ms ; envoi après commit (`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` sur un exécuteur borné) ; nouvel essai avec délai. | Avec un SMTP bouchonné qui ne répond pas, `register` répond en moins de 500 ms et le pool ne s'épuise pas. | M |
| OPS-03 | Moyenne | `GameDataLoader.run` n'est pas transactionnel (`GameDataLoader.java:77-83`) et le test `count() > 0` saute toute étape déjà commencée (`:222`). | Un crash entre les passes 1 et 2 laisse 353 chapitres sans liens, définitivement. | `@Transactional` sur le chargement, et contrôle d'intégrité au démarrage (353 chapitres, aucun chapitre sans lien hors 350) ; voir aussi DATA-00. | Crash simulé en passe 2 → base vide et rechargement complet au redémarrage suivant. | S |
| OPS-04 | Haute | Ni logs structurés, ni métriques, ni alertes : seul `/actuator/health` est exposé. | Panne ou pic de 500 découvert par les joueurs. | Logs JSON (`logging.structured.format.console`), métriques actuator, suivi des erreurs (Sentry ou équivalent), sonde de disponibilité externe et alerte sur le taux de 5xx. | Une 500 provoquée déclenche une alerte en moins de 5 min. | M |
| OPS-05 | Moyenne (premier mois) | Aucune purge : un refresh token par connexion et par rafraîchissement (access token de 15 min), plus les tokens de vérification et de réinitialisation. | Hypothèse de 10 rafraîchissements par jour : 1 000 joueurs actifs ≈ 10 000 lignes par jour, soit 3,6 millions par an. Cela pèse sur une base Render à stockage limité. **Nécessaire dans le premier mois.** | `@EnableScheduling` et job quotidien : supprimer les tokens expirés depuis plus de 7 jours (garder les révoqués jusqu'à leur expiration pour la détection de réutilisation) et les comptes non vérifiés de plus de 7 jours. ShedLock seulement si plusieurs instances. | Test : après le job, plus aucun token expiré depuis plus de 7 jours. | S |
| OPS-06 | Haute | Sauvegardes, restauration et retour arrière non définis ; les changesets SQL n'ont pas de rollback. | Perte de parties sans procédure testée. | Vérifier ce que le plan Render inclut (sauvegardes quotidiennes, restauration à un instant donné, expiration des bases gratuites) ; restauration testée une fois par trimestre ; migrations rétrocompatibles (ajouter, migrer, supprimer) pour déployer sans coupure ; script de rollback pour chaque changeset de données. | Restauration réussie sur un environnement de test, durée mesurée. | M |
| OPS-07 | Basse | `listerInventaire` est appelé plusieurs fois par action (ConditionService, EffetChapitreService, InventaireService, PersonnageMapper) ; N+1 sur la liste des personnages (`PartieService.java:191-199` + `PersonnageMapper.java:24`). | Pour 100 et 1 000 joueurs : sans effet. Pour 10 000 joueurs actifs, le pool de 10 connexions devient la limite. | Mémoriser l'inventaire dans un contexte de requête ; `@BatchSize` ; pool de 20 et instance de 1 Go au-delà de 1 000 joueurs simultanés. | Au plus 15 requêtes SQL pour `POST /chapitre/{id}`, mesuré avec les statistiques Hibernate. | M |
| QUAL-01 | Haute | 495 tests, surtout avec des mocks ; aucun test sur une vraie base PostgreSQL (migrations, rollback de SEC-04), aucun test données-contre-livre. La CI Sonar lance `mvn verify` (`.github/workflows/sonar.yml:55-56`). Les tests n'ont pas pu être exécutés ici. | Régressions de données et de transactions invisibles. | Testcontainers PostgreSQL + tests de la section 8. | La CI bloque la fusion si un test échoue. | M |
| QUAL-02 | Moyenne | Pas de version d'API ni d'OpenAPI ; les apps mobiles installées ne se mettent pas à jour de force. | Un changement de contrat casse les anciennes versions de l'app. | Préfixe `/api/v1`, documentation OpenAPI générée, endpoint `/version` avec version minimale de l'app. | Contrat OpenAPI publié et vérifié en CI. | M |
| QUAL-03 | Basse | Noms mélangés (routes `/auth/register` en anglais, services en français), coquille `viel_homme_fou`, commentaires obsolètes (« combat non construit », `EffetChapitreService.java:93`). | Dette de lisibilité. | Convention unique définie avant la v1 publique. | — | S |
| QUAL-04 | Haute (MVP2) | Le coin est un objet à 999 exemplaires par personnage, jamais décompté (`PersonnageService.java:171`, `:353-356`). | Impossible de vendre des coins sans refonte ; possibilité de le jeter via `DELETE /objets/coin`. | Portefeuille par utilisateur (journal des mouvements avec clé d'idempotence), validation serveur des reçus Google Play et App Store, coin retiré de l'inventaire. | Un achat rejoué deux fois ne crédite qu'une fois. | L |
| QUAL-05 | Moyenne (MVP2) | L'identifiant de chapitre est un entier global (`Chapitre.java:37-38`), les sous-sections sont codées 2101/2102, objets et ennemis n'ont pas de tome, et les types sont figés par des contraintes CHECK (`001-baseline.sql:73`, `:99`). | Collision dès le tome 2 ; report des Objets Spéciaux et de la 6e discipline impossible. | Colonne `livre` (clé composée livre + numéro), champ « sous-section », `personnage.livre`, import d'un personnage d'un tome à l'autre. | Un tome 2 fictif de 3 sections se charge à côté du tome 1. | L |

## 5. Écarts VO/VF

Les 350 sections ont été lues en parallèle. Aucun nombre ne diffère entre les deux éditions : stats d'ennemis, pertes, montants et plages de hasard sont identiques. Les écarts viennent presque tous des corrections de Project Aon (errata.htm), absentes du livre Gallimard de 1985. `chapitre.json` suit la VO partout, sauf au 201.

| Section | VF (Gallimard) | VO (Project Aon) | chapitre.json suit | Version retenue et raison |
| --- | --- | --- | --- | --- |
| 17 | Tirage 3-9 → 331 | 3-9 → 316 | VO | VO : le 316 décrit la dégringolade, comme au 89 qui a le même tirage (errata). |
| 20 | → 272 | → 273 | VO | VO : on vient de la péniche (190 → 20) ; le 273 (fortifications au bord de la rivière) prolonge le 190, alors que le 272 est le sentier du Bois des Brumes. |
| 58 | Se plaquer contre les rochers → 286 (mort) | → 251 | VO | VO : le 251 n'a aucune autre entrée et renvoie au 10 comme le 160 ; le choix prudent ne doit pas tuer (errata). |
| 67 | → 140 | → 252 | VO | VO : l'Orientation déconseille le sud (ours du 140) et choisit l'est ; au 222, l'est mène au 252. |
| 151 | Seul choix : 87 | Ajoute « retirer la broche → 337 » | VO | VO : sinon le 151 n'offre qu'une voie, alors que le 23 propose le 337 (errata). |
| 201 | Est → 215 | Est → 15 | **VF** | VO recommandée (DATA-08) : le 15 renvoie au 201 vers l'ouest, ce qui boucle la carte. Retenu (D-02). |
| 250 | Seul choix : suivre les Kakarmis → 186 | Ajoute « continuer → 228 » | VO | VO : symétrique du 187, qui propose déjà 186 ou 228. |
| 181 | « Vous avez perdu votre Équipement » | Phrase supprimée | VF (vol du sac) | VO : on arrive au 181 par le 22 sans rien avoir perdu ; la perte appartient au 205 (DATA-06, DATA-07). |
| 205 | Le chef « ramasse votre Équipement » | Ajoute « effacez toutes les Armes et tous les objets du Sac » | Ni l'une ni l'autre (aucun effet) | VO, explicite (DATA-06). |
| 144 | Vol d'un objet du Sac, d'une arme « si vous n'avez plus de Sac » | … « ou plus aucun objet dans le Sac » | Aucune (vol au choix, toute catégorie) | VO, plus précise (REGLE-09). |
| 137 | Les 20 Pierres « prennent place dans votre bourse » | « un seul objet du Sac à Dos » | VF (BOURSE × 20) | VO retenue (D-04) : un objet de Sac à Dos. |
| 164 | Alether en « Objets Spéciaux » | Non précisé (donc Sac à Dos) | VO | VO retenue (D-05) : Sac à Dos. |
| 76 | Pierre de Vordak dans « une poche de votre tunique » | « dans votre Sac à Dos » (errata) | VO | VO, cohérente avec le 304 où les deux éditions disent Sac à Dos. |
| 236 | La Pierre est détruite (perte implicite) | Ajoute « effacez la Pierre de Vordak » | Les deux (−1) | Identique en jeu. |
| Règles | Potion « après un combat » | Ajoute « pas juste avant un combat » et « une seule arme à la fois en combat » | — | Les deux ajouts VO sont compatibles avec la VF ; les retenir (REGLE-05). |
| Notes PA | Absentes | 113 : le Laumspur peut servir de Repas ; 147 : variante du texte si l'on vient du 42 ; 347 : autant de Torches que voulu ; 349 : Pendentif facultatif | 113 non, 347 non (1 Torche), 349 non (obligatoire) | VF retenue : les notes n'existent pas dans le livre de 1984. Confirmé (D-09). |

Différences cosmétiques (aucun effet sur le jeu) :

| Section(s) | VF | VO |
| --- | --- | --- |
| 97, 183, 216 | Prince Pellagayo | Prince Pelathar |
| Partout | Glok, Gluâtre des Profondeurs, Loup Maudit | Giak, Burrowcrawler, Doomwolf |
| 228, 276, 297, 343 | « votre Hache » | « votre arme » (errata) |
| 58 | « en tirant votre épée » | « draw your weapon » |
| 88 | Épée et bouclier du soldat | Lance et bouclier (non ramassables) |
| Table des armes | Sabre, Glaive | Short Sword, Broadsword |
| 217, 310 | Enseigne « KOLANIS Herboriste et Sage » ; pancarte vide | Enseignes en image, texte absent |
| 1, 4, 18, 23… | Choix de discipline en dernier | Choix de discipline en premier (errata) |
| 21, 32, 46 | Distances en kilomètres | Distances en miles |

## 6. Plan de livraison

| Lot | Exigences | Effort cumulé |
| --- | --- | --- |
| 1. Bloquant avant lancement | SEC-01, SEC-02, SEC-03, SEC-04, OPS-02, OPS-04, OPS-06 ; REGLE-01, REGLE-02, REGLE-03, REGLE-05, REGLE-08 ; DATA-01 à DATA-05, DATA-09 ; RGPD-01 (suppression de la date de naissance, D-08) et RGPD-04 ; QUAL-01 limité aux tests de la section 8 | Environ 3 à 4 semaines pour une personne |
| 2. Premier mois | OPS-05 (purge), OPS-01, OPS-03, SEC-05 à SEC-08, RGPD-02, RGPD-03 ; REGLE-06, REGLE-07, REGLE-09, REGLE-11, REGLE-12 (Sac à Dos, D-06), REGLE-13, REGLE-15 (coin décompté, D-07), REGLE-14 ; DATA-06 à DATA-08, DATA-10, DATA-11, DATA-13, DATA-14 (D-04), DATA-16 (D-10) ; DATA-00 (catalogue versionné) ; QUAL-02 ; aide du combat pour REGLE-04 (D-01) | Environ 4 semaines |
| 3. Plus tard | REGLE-10 et DATA-12 (283), OPS-07, QUAL-03, QUAL-04 et QUAL-05 (MVP2) | À chiffrer avec le MVP2 |

Ordre conseillé dans le lot 1 : d'abord DATA-00 ou un premier changeset Liquibase, pour que toutes les corrections de données suivent le même chemin ; puis SEC-03 et SEC-01, qui coûtent moins d'une journée.

## 7. Décisions prises

| ID | Question | Décision (25/09/2026) | Conséquence sur le cahier des charges |
| --- | --- | --- | --- |
| D-01 | Actions DEFENSE et OBJET | Garder tel quel, comme règle maison | REGLE-04 devient un écart assumé, à documenter dans l'aide du combat. Aux 231 et 339, DEFENSE et OBJET continuent de compter comme assauts pour les seuils « 4 assauts ». REGLE-05 reste nécessaire pour `/consommer` hors action OBJET. |
| D-02 | Édition de référence pour les renvois | VO Project Aon | DATA-08 retenu (201 → 15). Aucune autre correction de lien. |
| D-03 | Perte de l'équipement | Au 205 seulement | DATA-06 et DATA-07 retenus. |
| D-04 | 20 Pierres du 137 | Un seul objet de Sac à Dos | DATA-14 : nouvel objet `pierres_tombeau` (catégorie OBJET, quantité 1) au 137 ; `pierre_precieuse` n'est plus utilisé. |
| D-05 | Essence d'Alether | Sac à Dos (actuel) | DATA-15 sans changement. |
| D-06 | Perte du Sac à Dos | Modéliser, fidèle au livre | REGLE-12 passe au lot 2 : booléen `possedeSac`, perdu aux 174, 188 (tirage 0-6), 258 et 294 ; nouvel objet `sac_a_dos` ramassable aux 20 et 62. |
| D-07 | Coût de la mort en MVP1 | Coin décompté dès le MVP1 | REGLE-15 (Moyenne, lot 2) : retirer 1 coin à chaque retour après défaite ou résurrection, refuser à 0 coin. Stock initial maintenu à 999 pendant la bêta, pour ne bloquer aucun testeur ; le décompte sert à tester le mécanisme du MVP2. La restauration de l'ENDURANCE reste inchangée. |
| D-08 | Date de naissance | Supprimer le champ | RGPD-01 : retrait de l'API, de l'entité et de la colonne (migration `DROP COLUMN`, qui efface aussi les dates déjà collectées). |
| D-09 | Note PA du 113 (Laumspur comme Repas) | Non, VF | Aucun changement. |
| D-10 | 4 Repas du 184 | 1 obligatoire et 3 facultatifs, sans changer l'ordre d'application | DATA-16 ajouté. Petit changement de code : deux lignes `repas` pour la même section, alors que `ramasserObjetDuChapitre` prend la première trouvée (`PersonnageService.java:509-513`) et que `objet_chapitre_ramasse` est unique par (personnage, chapitre, objet) (`001-baseline.sql:418`). Le plafond de ramassage doit devenir la somme des lignes de l'objet. |

## 8. Tests à écrire

Un test par exigence critique ou haute. Les tests « données » lisent les quatre JSON en JUnit pur, sans Spring ; les tests « intégration » utilisent Testcontainers PostgreSQL.

| Exigence | Type | Test |
| --- | --- | --- |
| REGLE-01 | Unitaire | `TableCombatServiceTest.tableConformeAuLivre` : fixture de 130 couples (E, LS) transcrite de crtneg.png et crtpos.png, comparaison cellule par cellule. |
| REGLE-02 | Unitaire | `CombatServiceTest.unSeulTiragePourLesDeuxCamps` : `Random` bouchonné renvoyant 3 ; vérifier un seul appel et les pertes E et LS de la même cellule, y compris quand l'ennemi meurt. |
| REGLE-03 | Unitaire | `CombatServiceTest.fuiteInfligeLaPerteLSSeulement`. |
| REGLE-04 | Unitaire | `defenseCompteCommeAssaut : aux 231 et 339, vérifier que DEFENSE et OBJET font avancer le compteur d'assauts, comme décidé en D-01`. |
| REGLE-05 | Intégration | `PersonnageControllerTest.consommerPotionPendantCombatRefuse` (400). |
| DATA-01 à 04 | Données | `StatsEnnemisConformesAuLivreTest` : table attendue section → \[(HAB, END)\] pour les 29 combats, dont 133 = (16, 18), 220 = (11, 20), 229 = (16, 25), 340 = (14, 24). |
| DATA-05 | Données | `LiensDeCombatTest` : aucun lien `fuite=0` dont le texte commence par « Si vous êtes vainqueur » ; 112 → 248 exige VICTOIRE. |
| DATA (global) | Données | `CoherenceLivreTest` : les liens de chaque section = les renvois « rendez-vous au N » du texte (plus les coins et les exceptions VO listées) ; les plages de hasard couvrent 0 à 9 sans chevauchement ; chaque « perdez N points d'ENDURANCE » a son effet. |
| SEC-01 | Intégration | `CreationPersonnageTest.tirageServeur` : 1 000 créations donnent HAB ∈ \[10, 19\] et END ∈ \[20, 29\] ; un corps avec `hasardHabilite = 1000` n'a aucun effet (ou renvoie 400). |
| SEC-02 | Intégration | `LimitationDebitTest` : 4e `forgot-password` pour un même email en une heure → 429 ; 11e login par IP et par minute → 429. |
| SEC-03 | Intégration | `DemarrageProdTest` : le contexte refuse de démarrer en profil `prod` sans `JWT_SECRET`. |
| SEC-04 | Intégration (PostgreSQL) | `RefreshTokenReutilisationIT` : rotation, réutilisation de l'ancien token, puis `SELECT count(*) FROM refresh_token WHERE revoked = false AND utilisateur_id = ?` = 0. |
| OPS-02 | Intégration | `EmailAsynchroneIT` : SMTP bouchonné bloquant ; `register` < 500 ms et `HikariPool.activeConnections` revenu à 0. |
| OPS-04, OPS-06 | Procédure | Alerte déclenchée par une 500 provoquée ; restauration d'une sauvegarde sur un environnement de test, chronométrée. |
| RGPD-01 | Intégration | La date de naissance n'existe plus ni dans l'API ni en base. |
| QUAL-01 | CI | La CI exécute les tests Testcontainers et bloque la fusion en cas d'échec. |
