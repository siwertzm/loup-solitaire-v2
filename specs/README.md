# Specs — corrections Loup Solitaire (Tome 1)

Une tâche = un dossier `lot-N/<ID>-<nom>/spec.md`, avec le problème, ce qu'il faut faire, les critères d'acceptation, les tests et la branche à créer (`fix-<ID>` ou `feature-<ID>`, voir `CLAUDE.md`).

- **Source** : `source/` contient le cahier des charges d'origine (audit du 25/09/2026) et les trois listes de lots. Le détail des constats, les écarts VO/VF (section 5) et les décisions D-01 à D-10 (section 7) y restent.
- **Ajouts** : SEC-09, QUAL-06 et OPS-08 viennent de l'audit Claude Code du 11/11/2026. SEC-06 et OPS-05 existaient déjà et ont été complétés.
- **Second audit du 11/11/2026** (textes, i18n, règles `CLAUDE.md`) : SEC-10, QUAL-07, QUAL-08, REGLE-16 et QUAL-09 ajoutés ; QUAL-03 complété. DATA-17 vient de la comparaison complète des textes avec la VF en ligne chg96 et la VO Project Aon.
- **Audits Claude Code du 16 et 17/11/2026** (repo entier) : SEC-12, QUAL-18 et QUAL-19 faits le 17/11 (PR #95 à #97) ; QUAL-20, QUAL-21 et SEC-13 ajoutés au lot 3.
- **Audit du déploiement Android du 17/11/2026** : lot « Bêta-test » (`beta-test/`), OPS-09 à OPS-12 ; corrections déjà faites le 17/11 listées dans « Avancement ».
- **Retours bêta-testeurs (16/11/2026)** : QUAL-12, QUAL-13, QUAL-16 ; QUAL-14 et QUAL-15 viennent du porteur du projet. Ajouts du porteur du projet pour le lot 3 : REGLE-18 et REGLE-19 (Guérison, édition augmentée), QUAL-17 (dialogue du combat). Les infos manquantes sont listées dans la section « À demander avant de commencer » de chaque spec.
- Chaque tâche garde son dossier une fois finie. Quand une tâche est finie : passer son statut à « Fait » dans son `spec.md`, la retirer du tableau « Tâches à faire » et l'ajouter dans « Avancement ».

## Avancement (17/11/2026)

**Lots 1 et 2 terminés.** Reste pour OPS-06 : un test de restauration chez Render (voir `backend/README.md`). Prochaines tâches : lot « Bêta-test » (OPS-09, OPS-10), lot 3.

**Fait** (statut « Fait » dans le `spec.md`, PR fusionnées jusqu'à la #77) :
- Lot 1 : SEC-09, SEC-10, OPS-08, QUAL-07, OPS-06 (test chez Render à faire).
- Lot 2, étape A : QUAL-02, DATA-00, OPS-03, SEC-08, OPS-01, SEC-07 ; QUAL-06 (tests frontend dans la CI).
- Lot 2, étape B : SEC-06, SEC-05, OPS-05, RGPD-03.
- Lot 2, étape C : REGLE-16, REGLE-13, REGLE-07, REGLE-14, REGLE-11 + DATA-13, DATA-06 + DATA-07, REGLE-12, REGLE-09, DATA-14, DATA-17.
- Lot 2, étape D : QUAL-08 (Pièces Premium du profil laissées à 999, à revoir avec REGLE-15), REGLE-04, QUAL-16, REGLE-17 (un seul Sac à Dos, ajoutée le 16/11/2026), QUAL-12, QUAL-10.
- Lot 3 : QUAL-17 (boîte de dialogue du combat agrandie), REGLE-10 + DATA-12 (malus du Vordak à partir du 2e assaut, affiché sur la carte), QUAL-03 (convention de nommage), REGLE-18 (pas de Guérison sur un chapitre à perte d'ENDURANCE ; on reste sur la VO), QUAL-15 (page Options : taille du texte), RGPD-02 (export des données par email), [SEC-11](lot-3/SEC-11-audit-securite/spec.md) (audit du 16/11 : compte deviné au temps de réponse, rafraîchissements simultanés), OPS-07 (requêtes SQL mesurées, N+1 de la liste des personnages supprimé), DATA-11 (décision D-11 : FUITE dès la première action aux 191, 220 et 339 ; type `evitement` pour partir sans combattre aux 112 et 180), [DATA-18](lot-3/DATA-18-112-apres-victoire/spec.md) (112 : le 248 aussi proposé après une victoire, `evitement=1`), [SEC-12](lot-3/SEC-12-plafond-personnages/spec.md) (au plus 20 personnages par compte), [QUAL-18](lot-3/QUAL-18-regles-en-double/spec.md) (gardes « mort » et protections écrites une seule fois), [QUAL-19](lot-3/QUAL-19-commentaires-et-readme/spec.md) (commentaires et README alignés : Pièce Premium non consommée), [QUAL-20](lot-3/QUAL-20-derniere-activite/spec.md) (« Dernière activité » mise à jour par toute action de jeu), [QUAL-21](lot-3/QUAL-21-tables-combat-synchronisees/spec.md) (tables de combat de la page des règles vérifiées contre le serveur), [SEC-13](lot-3/SEC-13-blocage-connexion-par-compte/spec.md) (blocage après mots de passe faux compté par compte).
- Bêta-test, corrections du 17/11/2026 (PR #104 à #110, sans spec) : SEC-14 (lien de vérification d'email ouvert dans l'app Android, `allowBackup` désactivé), QUAL-22 (icône et splash Android, splash iOS), RGPD-05 (page web de suppression du compte exigée par Google Play), QUAL-23 (version de l'app unique dans `package.json`), QUAL-24 et QUAL-25 (contenu au-dessus des boutons de navigation Android, fond des pages dessous). Google Play : compte, app, fiche et formulaires faits, test interne avec envoi automatique par la CI.
- Abandonné : REGLE-01 + REGLE-02 (16/11/2026, décision D-12 : combat maison gardé), DATA-05 (16/11/2026, décision D-11 : le 112 → 248 reste un départ sans combat), REGLE-06, DATA-16 (16/11/2026 : les 4 Repas du 184 restent automatiques, le joueur retire ceux qu'il ne veut pas), QUAL-14 (16/11/2026 : les URL du jeu gardent l'id du personnage). Reporté au lot 3 : RGPD-02, REGLE-15 (16/11/2026, avec QUAL-04).

## Tâches à faire

### Bêta-test — déploiement sur les stores

| ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- |
| [OPS-09](beta-test/OPS-09-test-ferme-android/spec.md) | Haute | S + 14 jours | Test fermé Android (12 testeurs / 14 jours), choix de la piste dans le workflow |
| [OPS-10](beta-test/OPS-10-testflight-ios/spec.md) | Haute | S | Version iOS : reprise de TestFlight (bloqué : compte Apple Developer), vérifications sur iPhone |
| [OPS-11](beta-test/OPS-11-production-android/spec.md) | Moyenne | S | Production Android, après OPS-09 |
| [OPS-12](beta-test/OPS-12-production-ios/spec.md) | Moyenne | M | Production iOS (App Store), après OPS-10 |

Ordre : OPS-09 et OPS-10 en parallèle (l'un attend 14 jours, l'autre le compte Apple), puis OPS-11 et OPS-12.

### Lot 1 — avant d'élargir le test TestFlight

Toutes les tâches du lot 1 sont faites. OPS-06 : le test de restauration chez Render reste à faire (porteur du projet).

### Lot 2 — premier mois

Toutes les tâches du lot 2 sont faites, abandonnées ou reportées au lot 3 (voir « Avancement »).

### Lot 3 — plus tard (MVP2, optimisation)

| ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- |
| [QUAL-04](lot-3/QUAL-04-portefeuille-coins/spec.md) | Haute (MVP2) | L | Portefeuille de coins |
| [REGLE-15](lot-3/REGLE-15-coin-decompte/spec.md) | Moyenne | M | Coin décompté à chaque retour ou résurrection (reporté du lot 2, à faire avec QUAL-04 ; met `CLAUDE.md` à jour) |
| [QUAL-05](lot-3/QUAL-05-multi-tomes/spec.md) | Moyenne (MVP2) | L | Modèle multi-tomes |
| [QUAL-09](lot-3/QUAL-09-choix-langue/spec.md) | Moyenne | M | Choix de la langue ; en.json corrigé |
| [QUAL-11](lot-3/QUAL-11-angular-22/spec.md) | Basse | M | Migration vers Angular 22 et Vitest 5 (`ng update`) |
| [REGLE-19](lot-3/REGLE-19-mode-expert/spec.md) | Basse | M/L | Mode expert : Guérison limitée à 10 points au total |
| [QUAL-13](lot-3/QUAL-13-bande-originale/spec.md) | Basse | M | Bande originale : socle de musique de fond — retour bêta |

### Ordre conseillé du lot 3 (proposé le 16/11/2026)

1. **QUAL-17** (dialogue du combat écrasé) : visible par les joueurs, petit.
2. **REGLE-10 + DATA-12** (283) : règle du jeu fausse.
3. **QUAL-03** (nommage) : avant les tâches qui touchent ces fichiers.
4. **REGLE-18 puis REGLE-19** (Guérison, mode expert) : même code ; questions ouvertes à trancher d'abord.
5. **QUAL-15** (page Options) : questions ouvertes.
6. **RGPD-02** (export des données) : après la page Options (décision du 16/11/2026) ; en attendant, demandes traitées à la main.
7. **OPS-07** (requêtes SQL), **QUAL-11** (Angular 22) : sans urgence. **QUAL-20, QUAL-21, SEC-13** (second audit du 17/11/2026) : petits, à glisser quand on veut ; QUAL-20 d'abord (visible sur l'accueil).
8. **QUAL-09** (choix de la langue, `en.json` corrigé) : juste avant la musique (décision du 16/11/2026).
9. **QUAL-13** (bande originale) : avant-dernière (décision du 16/11/2026) ; questions ouvertes (licence de la musique…).
10. **QUAL-04 + REGLE-15** (coins), **QUAL-05** (multi-tomes) : MVP2.

## Statuts vérifiés le 11/11/2026

Les statuts des listes de `source/` ont été contrôlés contre le code et `chapitre.json`.

**Corrigé :**
- **DATA-16** était marqué « Fait » : le 184 a toujours `repas` × 4 obligatoires et aucune migration ne le change. Remis à faire, puis abandonné le 16/11/2026 (voir « Avancement »).

**À confirmer :** plus rien (DATA-05 et DATA-11 tranchés par D-11, REGLE-01/02 par D-12).

**Abandonné :**
- **REGLE-01 / REGLE-02** (Table des Coups Portés, un seul tirage par assaut), décision D-12 du 16/11/2026 : le combat maison reste tel quel, volontairement différent du livre (`CLAUDE.md`).
- **DATA-05** (lien 112 → 248), décision D-11 du 16/11/2026 : le livre permet de partir sans combattre (nouveau type de condition `evitement`). Possible tant qu'aucun assaut n'est joué ; une fois engagé, il faut gagner ou utiliser FUITE. Test explicite dans `LiensDeCombatTest`.
- **REGLE-06** (interdire de jeter un objet pendant un combat), décision du 12/11/2026 : jeter un objet en plein combat ne peut que désavantager le joueur (ex. sa seule arme : −4 HABILETÉ), sans effet sur le déroulé de l'aventure. `DELETE /personnages/{id}/objets/{objetId}` reste permis en combat ; seule la consommation y est bloquée (REGLE-05).

**Vérifié « Fait » :** SEC-01 à SEC-04, OPS-02, OPS-04, REGLE-03, REGLE-05 (consommation bloquée pendant un combat en cours), REGLE-08, DATA-01 à DATA-04, DATA-08 à DATA-10, RGPD-01, RGPD-04, QUAL-01 (`./mvnw verify` vert avec Testcontainers, 600 tests).

## Ordre conseillé

Toutes les étapes sont faites (OPS-06 : test chez Render à faire) (voir « Avancement »).

1. **SEC-09** tout de suite : c'est le seul problème que les testeurs TestFlight vont voir. **SEC-10** dans la foulée (même symptôme côté Profil après 15 minutes).
2. **OPS-08** dans la même petite PR que le `.gitignore` (graphify-out, Zone.Identifier).
3. **OPS-06** avant d'ouvrir l'app au public.
4. Lot 2, dans l'ordre du tableau :
   - **A. Socle avant les changements de contrat et de données.** QUAL-02 d'abord (au moins `/version`) : les apps installées ne se mettent pas à jour seules, et SEC-06, QUAL-08, REGLE-12 et REGLE-15 changent le contrat API ; il faut pouvoir forcer la mise à jour avant. Puis DATA-00, qui évite une migration SQL à chaque correction DATA/REGLE suivante ; OPS-03 juste après (DATA-00 peut le remplacer : à trancher au récap de DATA-00). SEC-08, OPS-01 et SEC-07 sont courts et rendent la prod plus sûre (409 au lieu de 500, mémoire JVM, H2 hors du jar).
   - **B. Comptes et RGPD.** SEC-06 en premier : une faute de frappe dans l'e-mail bloque le compte, c'est le seul risque de perte de compte restant. SEC-05 après SEC-08 (même zone : inscription et 409). OPS-05 avant RGPD-03, qui s'appuie sur la purge pour ses durées de conservation.
   - **C. Règles du jeu.** D'abord le trou côté serveur où l'API acceptait une action interdite (REGLE-16 : échange répétable ; REGLE-06 abandonnée, voir plus haut), puis les corrections de code isolées (REGLE-13, REGLE-07), puis celles qui changent `chapitre.json` (REGLE-14, REGLE-11 + DATA-13, DATA-06 + DATA-07 ; DATA-16 abandonnée), simples après DATA-00. REGLE-12 (Sac à Dos) avant REGLE-09 et DATA-14, qui parlent des objets « de Sac ». DATA-17 (textes) en dernier, sans risque.
   - **D. Frontend.** REGLE-15 (coin décompté) est reportée au lot 3 avec QUAL-04. QUAL-10 en dernier, pour tester le front une fois les changements ci-dessus faits (la règle de `CLAUDE.md` impose déjà un test pour toute nouvelle logique en attendant). QUAL-12 et QUAL-16 (retours bêta, petits) peuvent se glisser à tout moment de l'étape D.
5. **Lot 3** : pas d'ordre imposé. QUAL-15 (page Options) avant ou avec QUAL-13 (bande originale), dont les réglages y vont. Plusieurs specs du lot 3 attendent des réponses (section « À demander avant de commencer »).
