# DATA-00 — Catalogue versionné : rechargement des JSON si leur empreinte change

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | M | `feature-DATA-00` | À faire |

Origine : Cahier des charges (DATA-00)

## Problème

Chaque correction de données demande aujourd'hui une modification du JSON **et** une migration SQL écrite à la main, qui doivent rester identiques. Risque de divergence entre une base neuve et la production.

## À faire

- Stocker en base une empreinte SHA-256 des quatre JSON.
- Si elle change au démarrage, `GameDataLoader` recharge dans une transaction les tables enfants (lien, cond, effet, objet_chap, chapitre_ennemi) et met à jour ennemi, objet et chapitre.
- Possible car les données des joueurs ne référencent que des identifiants stables.
- Peut remplacer OPS-03. À faire **avant** les autres corrections DATA du lot 2 pour qu'elles n'aient plus besoin de migration.

## Critères d'acceptation

- Modifier un JSON puis redémarrer met la base à jour sans migration ; sans modification, rien n'est rechargé.

## Tests

Test d'intégration (Testcontainers).

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/config/GameDataLoader.java`, migration pour la table d'empreinte
