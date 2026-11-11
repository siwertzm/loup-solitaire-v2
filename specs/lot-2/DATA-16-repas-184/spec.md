# DATA-16 — 184 : 1 Repas obligatoire + 3 facultatifs

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Basse | S | `fix-DATA-16` | À faire |

Origine : Cahier des charges (DATA-16, décision D-10). **Statut corrigé : marqué « Fait », mais non fait.**

## Problème

`chapitre.json` contient toujours `repas` × 4 **obligatoires** au 184, et aucune migration (`012-stats-ennemis.sql` couvre 62 mais pas 184). Le livre : « si vous souhaitez conserver l'une ou l'autre de ces trouvailles », puis un Repas imposé.

## À faire

- `chapitre.json` : deux lignes `repas` au 184, une `valeur 1, optionnel false` et une `valeur 3, optionnel true` ; ordre d'application inchangé (objets obligatoires avant l'effet REPAS).
- Code : `PersonnageService.ramasserObjetDuChapitre` prend la première ligne trouvée et `objet_chapitre_ramasse` est unique par (personnage, chapitre, objet). Le plafond de ramassage doit devenir la **somme** des lignes de l'objet.
- Migration Liquibase (avec `rollback`) dans le même commit que le JSON.

## Critères d'acceptation

- En arrivant au 184 sans Repas : le Repas obligatoire est consommé, pas de −3 ENDURANCE, et 3 Repas restent à prendre au choix.

## Tests

`PersonnageServiceTest` (ramassage sur deux lignes) ; `CoherenceLivreTest` reste vert.

## Fichiers concernés

`backend/src/main/resources/data/chapitre.json`, `backend/src/main/java/com/loupsolitaire/backend/service/PersonnageService.java`, nouvelle migration
