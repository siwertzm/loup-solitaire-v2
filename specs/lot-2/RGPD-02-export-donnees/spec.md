# RGPD-02 — Export des données du compte en JSON

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `feature-RGPD-02` | À faire |

Origine : Cahier des charges (RGPD-02)

## Problème

Pas d'export des données ; `GET /auth/me` ne couvre ni l'inventaire ni le journal. Droit d'accès et de portabilité non outillé.

## À faire

- `GET /auth/me/export` : profil, personnages, inventaires, journaux, en JSON (DTO `response/`, jamais d'entité).
- Bouton dans la page profil.

## Critères d'acceptation

- L'export contient toutes les données du compte.

## Tests

Test de contrôleur.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/controller/AuthController.java`, `CompteService.java`
