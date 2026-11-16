# SEC-12 — Au plus 20 personnages par compte

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Moyenne | S | `fix-SEC-12` | Fait |

Origine : audit Claude Code du 16/11/2026 (point 1)

## Problème

Aucune limite au nombre de personnages par compte, et seules les routes `/auth/**` sont limitées en débit. Un script sur un seul compte vérifié pouvait enchaîner `POST /personnages/tirage` et `POST /personnages` (une dizaine de lignes en base par personnage) et remplir la base ; `GET /personnages` rechargeait ensuite tout à chaque accueil.

## Réalisé

- `PartieService.verifierPlaceLibre` : `400` sur le tirage et sur la création à partir de 20 personnages (`MAX_PERSONNAGES_PAR_COMPTE`). Appelé après le verrou de l'utilisateur : deux créations simultanées ne peuvent pas dépasser la limite.
- `PersonnageRepository.countByUtilisateur`.
- Écran de création : un `400` au tirage affiche « Vous avez atteint le nombre maximal de personnages… » (`fr.json`, `en.json`).
- `backend/README.md` : règle ajoutée au modèle de jeu.

## Tests

`PartieServiceTest.refuseTirageEtCreationAuDelaDuPlafondDePersonnages`. Vérifié en local : 20e personnage créé, 21e refusé avec le message, de nouveau possible après une suppression.

## Fichiers concernés

`PartieService.java`, `PersonnageRepository.java`, `creation-personnage.page.ts`, `fr.json`, `en.json`, `backend/README.md` — PR #95
