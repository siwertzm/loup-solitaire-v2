# Lot 2 — Premier mois

Ces 28 exigences suivent le lancement, dans le premier mois. Effort estimé : environ 4 semaines.

| ID | Sévérité | À faire | Effort | Statut |
| --- | --- | --- | --- | --- |
| OPS-05 | Moyenne | Purge quotidienne des tokens expirés et des comptes non vérifiés | S | À faire |
| OPS-01 | Moyenne | Réglage mémoire de la JVM (MaxRAMPercentage) et sondes de santé | S | À faire |
| OPS-03 | Moyenne | Chargement du livre transactionnel + contrôle d'intégrité au démarrage | S | À faire |
| SEC-05 | Moyenne | Réponses identiques à l'inscription et au mot de passe oublié (anti-énumération) | S | À faire |
| SEC-06 | Moyenne | Mot de passe exigé pour changer d'email ; ancien email actif jusqu'à confirmation | M | À faire |
| SEC-07 | Basse | H2 en scope test ; contrôle des CVE dans la CI | S | À faire |
| SEC-08 | Basse | 409 sur violation d'unicité, 500 générique journalisé | S | À faire |
| RGPD-02 | Moyenne | Export des données du compte en JSON | S | À faire |
| RGPD-03 | Moyenne | Durées de conservation ; emails masqués dans les logs | S | À faire |
| REGLE-04 | Basse | DEFENSE et OBJET gardées : les décrire dans l'aide du combat — D-01 | S | À faire |
| REGLE-06 | Moyenne | Interdire de jeter un objet pendant un combat | S | À faire |
| REGLE-07 | Moyenne | Table 0-9 de la Maîtrise des Armes (Épée sur 5 et 7) | S | À faire |
| REGLE-09 | Moyenne | Vol du 144 : objet du Sac d'abord, arme seulement si le Sac est vide | S | À faire |
| REGLE-11 | Moyenne | 260 : combat à mains nues sans cumul des −4 | S | À faire |
| REGLE-12 | Moyenne | Modéliser le Sac à Dos (perte aux 174, 188, 258, 294 ; Sac aux 20 et 62) — D-06 | M | À faire |
| REGLE-13 | Basse | Pas de Guérison sur les sous-sections 2101 et 2102 | S | À faire |
| REGLE-14 | Basse | Choix « Sinon » exclusifs (valeur −1 sur les liens) | S | À faire |
| REGLE-15 | Moyenne | Retirer 1 coin par retour ou résurrection, stock de 999 en bêta — D-07 | M | À faire |
| DATA-00 | Moyenne | Catalogue versionné : rechargement des JSON si leur empreinte change | M | À faire |
| DATA-06 | Moyenne | 205 : perte des armes et du Sac (vol 10) — D-03 | S | À faire |
| DATA-07 | Moyenne | 181 : suppression du vol du Sac — D-03 | S | À faire |
| DATA-08 | Moyenne | 201 : lien est vers 15 (VO) — D-02 | S | Fait |
| DATA-10 | Basse | 62 : 3 Repas facultatifs | S | Fait |
| DATA-11 | Basse | 339 : fuite possible à tout moment (fuite=0) | S | Déja OK |
| DATA-13 | Moyenne | 260 : marqueur « sans arme » à la place de l'effet −4 (avec REGLE-11) | S | À faire |
| DATA-14 | Moyenne | 137 : 20 Pierres = un seul objet de Sac (pierres\_tombeau) — D-04 | S | À faire |
| DATA-16 | Basse | 184 : 1 Repas obligatoire + 3 facultatifs, ramassage sur plusieurs lignes — D-10 | S | Fait |
| QUAL-02 | Moyenne | Préfixe /api/v1, OpenAPI, endpoint de version minimale | M | À faire |
