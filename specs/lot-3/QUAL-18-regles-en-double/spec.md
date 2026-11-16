# QUAL-18 — Règles de mort et de protection écrites une seule fois

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | S | `fix-QUAL-18` | Fait |

Origine : audit Claude Code du 16/11/2026 (points 2 et 3) ; déjà noté « hors périmètre » de SEC-11

## Problème

- La garde « personnage mort » était recopiée dans 4 services, avec un message qui oubliait la sortie `revenir-apres-defaite` après une mort en combat.
- La règle « sauf si vous possédez » des effets d'HABILETÉ (ex. −2 sans Bouclier Psychique) était écrite deux fois : à l'arrivée sur le chapitre (`EffetChapitreService`) et à chaque assaut (`CombatService`). La recherche « le personnage a-t-il la discipline X » était recodée 6 fois.

## Réalisé

- `Personnage.verifierVivant()` (message citant les deux sorties) et `Personnage.possedeDiscipline(IdDiscipline)`.
- `ConditionService.protectionsToutesPossedees(personnage, conditions)`, utilisée à l'arrivée et à chaque assaut ; inventaire lu une seule fois par vérification.
- Aucune règle ni contrat d'API changé (seul le texte du message « mort »).

## Tests

`ConditionServiceTest` (toutes les protections possédées, une manquante, aucune protection) ; `EffetChapitreServiceTest` et `CombatServiceTest` adaptés. Vérifié en local : 121 → 342 et 121 → 283, avec et sans Bouclier Psychique.

## Fichiers concernés

`Personnage.java`, `ConditionService.java`, `CombatService.java`, `EffetChapitreService.java`, `ObjetService.java`, `PersonnageService.java` — PR #96
