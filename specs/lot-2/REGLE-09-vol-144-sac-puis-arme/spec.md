# REGLE-09 — Vol du 144 : objet du Sac d'abord, arme seulement si le Sac est vide

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-REGLE-09` | Fait |

Origine : Cahier des charges (REGLE-09)

## Problème

`EffetChapitreService.resoudreVolEnAttente` accepte une arme avec la portée TOUT même si le Sac contient des objets.

## Mise en œuvre (16/11/2026)

Pas de nouvelle valeur `PorteeVol` ni de migration : la portée `TOUT` ne sert qu'au 144 (le 277 est en portée `ARME`). Sa règle devient « un objet ou repas du Sac ; une arme seulement si le Sac n'en contient aucun » (`EffetChapitreService.resoudreVolEnAttente`, 400 sinon). La popup du frontend ne propose que les choix valides (`chapitre-effets`, `itemsEligiblesVol`). Avec REGLE-12, « plus de Sac » = Sac vide.

## À faire (spec d'origine)

- Nouvelle valeur `PorteeVol.SAC_PUIS_ARME` : arme refusée tant qu'un objet OBJET/REPAS est possédé. Mettre à jour la contrainte CHECK si la colonne en a une (migration).

## Critères d'acceptation

- Sac non vide, choix d'une arme → 400.

## Tests

`EffetChapitreServiceTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/EffetChapitreService.java`, `model/enums/PorteeVol.java`
