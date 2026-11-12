# Specs — corrections Loup Solitaire (Tome 1)

Une tâche = un dossier `lot-N/<ID>-<nom>/spec.md`, avec le problème, ce qu'il faut faire, les critères d'acceptation, les tests et la branche à créer (`fix-<ID>` ou `feature-<ID>`, voir `CLAUDE.md`).

- **Source** : `source/` contient le cahier des charges d'origine (audit du 25/09/2026) et les trois listes de lots. Le détail des constats, les écarts VO/VF (section 5) et les décisions D-01 à D-10 (section 7) y restent.
- **Ajouts** : SEC-09, QUAL-06 et OPS-08 viennent de l'audit Claude Code du 11/11/2026. SEC-06 et OPS-05 existaient déjà et ont été complétés.
- **Second audit du 11/11/2026** (textes, i18n, règles `CLAUDE.md`) : SEC-10, QUAL-07, QUAL-08, REGLE-16 et QUAL-09 ajoutés ; QUAL-03 complété. DATA-17 vient de la comparaison complète des textes avec la VF en ligne chg96 et la VO Project Aon.
- Seules les tâches **à faire** ont un dossier. Quand une tâche est finie, passer son statut à « Fait » dans son `spec.md` et dans le tableau ci-dessous.

## Tâches à faire

### Lot 1 — avant d'élargir le test TestFlight

| ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- |
| [OPS-06](lot-1/OPS-06-sauvegardes-restauration/spec.md) | Haute | M | Sauvegardes, restauration et retour arrière |

### Lot 2 — premier mois

Tâches classées dans l'ordre conseillé (revue du 12/11/2026). L'étape renvoie à la section « Ordre conseillé » plus bas.

| # | Étape | ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- | --- | --- |
| 14 | C | [REGLE-13](lot-2/REGLE-13-guerison-sous-sections/spec.md) | Basse | S | Pas de Guérison sur 2101/2102 |
| 15 | C | [REGLE-07](lot-2/REGLE-07-table-maitrise-armes/spec.md) | Moyenne | S | Table 0-9 de la Maîtrise des Armes |
| 16 | C | [REGLE-14](lot-2/REGLE-14-choix-sinon-exclusifs/spec.md) | Basse | S | Choix « Sinon » exclusifs |
| 17 | C | [REGLE-11](lot-2/REGLE-11-mains-nues-260/spec.md) + [DATA-13](lot-2/DATA-13-marqueur-sans-arme-260/spec.md) | Moyenne | S | 260 : mains nues sans cumul |
| 18 | C | [DATA-06](lot-2/DATA-06-perte-equipement-205/spec.md) + [DATA-07](lot-2/DATA-07-pas-de-vol-181/spec.md) | Moyenne | S | Perte d'équipement au 205, pas au 181 |
| 19 | C | [DATA-16](lot-2/DATA-16-repas-184/spec.md) | Basse | S | 184 : 1 Repas obligatoire + 3 facultatifs |
| 20 | C | [REGLE-12](lot-2/REGLE-12-sac-a-dos/spec.md) | Moyenne | M | Modéliser le Sac à Dos |
| 21 | C | [REGLE-09](lot-2/REGLE-09-vol-144-sac-puis-arme/spec.md) | Moyenne | S | Vol du 144 : Sac d'abord |
| 22 | C | [DATA-14](lot-2/DATA-14-pierres-137/spec.md) | Moyenne | S | 137 : Pierres = 1 objet de Sac |
| 23 | C | [DATA-17](lot-2/DATA-17-textes-du-livre/spec.md) | Basse | S | Fautes et écarts dans le texte des chapitres (comparaison chg96 + VO) |
| 24 | D | [QUAL-08](lot-2/QUAL-08-textes-en-dur/spec.md) | Moyenne | S | Textes en dur ; messages du serveur affichés tels quels |
| 25 | D | [REGLE-15](lot-2/REGLE-15-coin-decompte/spec.md) | Moyenne | M | Coin décompté (met `CLAUDE.md` à jour) |
| 26 | D | [REGLE-04](lot-2/REGLE-04-aide-combat-regle-maison/spec.md) | Basse | S | Aide du combat : DEFENSE et OBJET |
| 27 | D | [QUAL-10](lot-2/QUAL-10-tests-frontend/spec.md) | Moyenne | M | Tests du frontend existant (guard, services, logique de jeu) |

### Lot 3 — plus tard (MVP2, optimisation)

| ID | Sévérité | Effort | Tâche |
| --- | --- | --- | --- |
| [REGLE-10](lot-3/REGLE-10-assaut-min/spec.md) + [DATA-12](lot-3/DATA-12-vordak-283/spec.md) | Basse | M | ASSAUT_MIN, Vordak du 283 |
| [OPS-07](lot-3/OPS-07-requetes-sql/spec.md) | Basse | M | Moins de requêtes SQL par action |
| [QUAL-03](lot-3/QUAL-03-nommage/spec.md) | Basse | S | Convention de nommage |
| [QUAL-04](lot-3/QUAL-04-portefeuille-coins/spec.md) | Haute (MVP2) | L | Portefeuille de coins |
| [QUAL-05](lot-3/QUAL-05-multi-tomes/spec.md) | Moyenne (MVP2) | L | Modèle multi-tomes |
| [QUAL-09](lot-3/QUAL-09-choix-langue/spec.md) | Moyenne | M | Choix de la langue ; en.json corrigé |
| [QUAL-11](lot-3/QUAL-11-angular-22/spec.md) | Basse | M | Migration vers Angular 22 et Vitest 5 (`ng update`) |
| [RGPD-02](lot-3/RGPD-02-export-donnees/spec.md) | Basse | S | Export des données en JSON (reporté du lot 2 : demandes traitées à la main en attendant) |

## Statuts vérifiés le 11/11/2026

Les statuts des listes de `source/` ont été contrôlés contre le code et `chapitre.json`.

**Corrigé :**
- **DATA-16** était marqué « Fait » : le 184 a toujours `repas` × 4 obligatoires et aucune migration ne le change. Remis à faire.

**À confirmer (marqués « Déjà OK », mais les données ne suivent pas le cahier) :**
- **DATA-05** : le lien 112 → 248 est toujours `fuite = 0`. Le code le traite exprès comme « combat évitable » (`PersonnageService.avancerVersChapitre`, chapitres 112 et 180). Si c'est un choix, l'écrire comme décision D-11 ; sinon, créer la spec.
- **DATA-11** : le lien 339 → 7 est toujours `fuite = 1`, le cahier demande `fuite = 0`. Même question.
- **REGLE-01 / REGLE-02** : le combat utilise toujours sa table maison et deux tirages par assaut. C'est cohérent avec `CLAUDE.md` (« système maison, volontairement différent du livre ») : à écrire comme décision D-11, pour que le cahier ne dise plus « Critique ».

**Abandonné :**
- **REGLE-06** (interdire de jeter un objet pendant un combat), décision du 12/11/2026 : jeter un objet en plein combat ne peut que désavantager le joueur (ex. sa seule arme : −4 HABILETÉ), sans effet sur le déroulé de l'aventure. `DELETE /personnages/{id}/objets/{objetId}` reste permis en combat ; seule la consommation y est bloquée (REGLE-05).

**Vérifié « Fait » :** SEC-01 à SEC-04, OPS-02, OPS-04, REGLE-03, REGLE-05 (consommation bloquée pendant un combat en cours), REGLE-08, DATA-01 à DATA-04, DATA-08 à DATA-10, RGPD-01, RGPD-04, QUAL-01 (`./mvnw verify` vert avec Testcontainers, 600 tests).

## Ordre conseillé

1. **SEC-09** tout de suite : c'est le seul problème que les testeurs TestFlight vont voir. **SEC-10** dans la foulée (même symptôme côté Profil après 15 minutes).
2. **OPS-08** dans la même petite PR que le `.gitignore` (graphify-out, Zone.Identifier).
3. **OPS-06** avant d'ouvrir l'app au public.
4. Lot 2, dans l'ordre du tableau :
   - **A. Socle avant les changements de contrat et de données.** QUAL-02 d'abord (au moins `/version`) : les apps installées ne se mettent pas à jour seules, et SEC-06, QUAL-08, REGLE-12 et REGLE-15 changent le contrat API ; il faut pouvoir forcer la mise à jour avant. Puis DATA-00, qui évite une migration SQL à chaque correction DATA/REGLE suivante ; OPS-03 juste après (DATA-00 peut le remplacer : à trancher au récap de DATA-00). SEC-08, OPS-01 et SEC-07 sont courts et rendent la prod plus sûre (409 au lieu de 500, mémoire JVM, H2 hors du jar).
   - **B. Comptes et RGPD.** SEC-06 en premier : une faute de frappe dans l'e-mail bloque le compte, c'est le seul risque de perte de compte restant. SEC-05 après SEC-08 (même zone : inscription et 409). OPS-05 avant RGPD-03, qui s'appuie sur la purge pour ses durées de conservation.
   - **C. Règles du jeu.** D'abord le trou côté serveur où l'API acceptait une action interdite (REGLE-16 : échange répétable ; REGLE-06 abandonnée, voir plus haut), puis les corrections de code isolées (REGLE-13, REGLE-07), puis celles qui changent `chapitre.json` (REGLE-14, REGLE-11 + DATA-13, DATA-06 + DATA-07, DATA-16), simples après DATA-00. REGLE-12 (Sac à Dos) avant REGLE-09 et DATA-14, qui parlent des objets « de Sac ». DATA-17 (textes) en dernier, sans risque.
   - **D. Frontend.** QUAL-08 avant REGLE-15 : le profil doit afficher le vrai nombre de coins avant qu'il commence à baisser. REGLE-15 change une règle de `CLAUDE.md` : à confirmer avant de la commencer. QUAL-10 en dernier, pour tester le front une fois les changements ci-dessus faits (la règle de `CLAUDE.md` impose déjà un test pour toute nouvelle logique en attendant).
