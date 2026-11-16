# QUAL-20 — « Dernière activité » pas mise à jour par certaines actions

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | XS | `fix-QUAL-20` | À faire |

Origine : second audit Claude Code du 17/11/2026 (point 1)

## Problème

L'accueil propose en tête (« à reprendre ») le personnage dont `derniereActivite` est la plus récente. Cette date est posée par `@PreUpdate` (`Personnage.surMiseAJour`), qui ne s'exécute que si la ligne du personnage change.

Les actions qui ne touchent que l'inventaire ou le combat (ramasser un objet, jeter un objet qui n'est pas une arme, DEFENSE à 0 dégât) ne changent pas cette ligne. Le chargement verrouillé (`OPTIMISTIC_FORCE_INCREMENT`, `PersonnageRepositoryCustomImpl`) n'incrémente alors que la version par une requête à part, sans `@PreUpdate`.

Exemple : on avance le personnage A, puis on ramasse des objets avec B. L'accueil propose toujours A.

Non vérifié en exécution lors de l'audit (raisonnement sur le fonctionnement d'Hibernate) : le test ci-dessous doit d'abord échouer.

## À faire

- Dans `findByIdPourModification`, juste après le verrou : `personnage.setDerniereActivite(Instant.now())` (toute action de jeu passe par là).
- Supprimer `@PreUpdate` / `surMiseAJour` et son commentaire.

## Critères d'acceptation

- Ramasser un objet (sans autre changement) met à jour `derniereActivite`.
- Une lecture (`GET`) ne la change pas.

## Tests

Un test d'intégration (PostgreSQL) : ramasser un objet optionnel → `derniereActivite` plus récente. Le lancer avant le correctif pour confirmer qu'il échoue.

## Fichiers concernés

`PersonnageRepositoryCustomImpl.java`, `Personnage.java`
