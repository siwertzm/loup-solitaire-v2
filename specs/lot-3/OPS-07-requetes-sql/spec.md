# OPS-07 — Moins de requêtes SQL par action

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | M | `fix-OPS-07` | Fait |

Origine : Cahier des charges (OPS-07)

## Problème

`listerInventaire` est appelé plusieurs fois par action ; N+1 sur la liste des personnages. Sans effet sous 1 000 joueurs ; le pool de 10 connexions devient la limite vers 10 000 joueurs actifs.

## À faire

- Mémoriser l'inventaire dans un contexte de requête (comme `ContexteConditions`) ; `@BatchSize` ; pool de 20 et instance de 1 Go au-delà de 1 000 joueurs simultanés.

## Réalisé (16/11/2026)

Mesures sur PostgreSQL (statistiques Hibernate, cache des chapitres vide = pire cas), personnage sans Chasse :

| Action | Avant | Après |
| --- | --- | --- |
| Avancer 1 → 85 (simple) | 11 | 11 |
| Avancer 82 → 235 (Repas) | 14 | 14 (13 cache chaud) |
| Avancer 24 → 184 (3 objets, 4 Repas obligatoires, 1 consommé) | 22 | 21 (20 cache chaud) |
| Afficher un chapitre | 11 | 11 (5 à 7 cache chaud) |
| Liste de 4 personnages | 6 (1 + N) | **3** |

- Liste des personnages (`GET /personnages`, `/auth/me`) : inventaires de tous les personnages en une requête (`PersonnageMapper.versReponses`) ; plus de N+1.
- Effet Repas : `InventaireService.retirerUnSiPossede`, une requête au lieu d'une lecture de tout l'inventaire puis d'une recherche.
- `RequetesParActionIT` : plafonds par action, échoue en CI si une requête en trop apparaît.

**Non fait (décision)** : le critère « au plus 15 requêtes » n'est pas atteint sur le 184 (21) : 5 sont des écritures, et le reste vient des lectures d'inventaire entre deux modifications (Hibernate vide les modifications en base avant chaque requête). Une mémoire de l'inventaire dans la requête demanderait de l'invalider à chaque ajout ou retrait : plus de code et de risque que de gain à la charge actuelle (quelques dizaines de joueurs, pool de 10 connexions). Pool de 20 connexions : pas nécessaire non plus. À reprendre si la charge dépasse 1 000 joueurs simultanés.

## Critères d'acceptation

- Au plus 15 requêtes SQL pour `POST /chapitre/{id}` (statistiques Hibernate).

## Tests

Test qui compte les requêtes.

## Fichiers concernés

services de jeu, mappers
