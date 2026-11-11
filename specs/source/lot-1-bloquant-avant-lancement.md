# Lot 1 — Bloquant avant lancement

Ces 21 exigences doivent être terminées avant l'ouverture au public. Effort estimé : 3 à 4 semaines pour une personne. Détail de chaque exigence dans l'onglet principal.

| ID | Sévérité | À faire | Effort | Statut |
| --- | --- | --- | --- | --- |
| SEC-01 | Critique | Tirages HABILETÉ/ENDURANCE côté serveur ; valider nom et disciplines | S | Fait |
| SEC-02 | Critique | Limitation de débit (Bucket4j) sur /auth/\*\* par IP et par email ; IP réelle derrière Render | M | Fait |
| SEC-03 | Critique | Aucun secret par défaut en prod (JWT\_SECRET, DB\_PASSWORD) ; démarrage refusé si absent | S | Fait |
| SEC-04 | Haute | Révocation de toutes les sessions conservée malgré l'exception (noRollbackFor ou REQUIRES\_NEW) | S | Fait |
| OPS-02 | Haute | Délais SMTP à 5 s et envoi des emails après commit, en asynchrone | M | Fait |
| OPS-04 | Haute | Logs JSON, métriques, suivi des erreurs, sonde externe et alerte sur les 5xx | M | Fait |
| OPS-06 | Haute | Sauvegardes vérifiées sur le plan Render, restauration testée, rollback des changesets | M | À faire |
| REGLE-01 | Critique | Table des Coups Portés officielle (130 couples E/LS) | S | Déja OK |
| REGLE-02 | Haute | Un seul tirage par assaut ; pertes des deux camps appliquées, même quand l'ennemi meurt | S | Déja OK |
| REGLE-03 | Haute | Fuite : l'assaut est joué, seul Loup Solitaire perd des points | S | Fait |
| REGLE-05 | Haute | Pas de potion via /consommer pendant un combat en cours ; Alether avant le 1er assaut | S | Fait |
| REGLE-08 | Moyenne | Ordre des ennemis multiples garanti (colonne ordre + @OrderColumn) | S | Fait |
| DATA-01 | Critique | 133 : Serpent Ailé 16/18 au lieu de 12/10 | S | Fait |
| DATA-02 | Critique | 229 : nouvel ennemi kraan\_229 à 16/25 | S | Fait |
| DATA-03 | Critique | 220 : nouvel ennemi garde\_corps\_220 à 11/20 | S | Fait |
| DATA-04 | Critique | 340 : nouvel ennemi glok\_loup\_maudit\_340 à 14/24 | S | Fait |
| DATA-05 | Haute | 112 : lien vers 248 soumis à la victoire (et non fuite=0) | S | Déja OK |
| DATA-09 | Moyenne | 291 : une seule Lance ramassable | S | Fait |
| RGPD-01 | Haute | Suppression de la date de naissance (API, entité, colonne) — D-08 | S | Fait |
| RGPD-04 | Moyenne | Politique de confidentialité, CGU, registre, accord Brevo | S | Fait |
| QUAL-01 | Haute | Testcontainers PostgreSQL + tests critiques de la section 8 | M | Fait |
