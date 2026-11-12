# RGPD-02 — Export des données du compte en JSON

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | S | `feature-RGPD-02` | À faire |

Origine : Cahier des charges (RGPD-02)

Reportée du lot 2 à la fin du lot 3 (décision du 12/11/2026) : le RGPD impose de répondre à une demande d'accès sous un mois, pas d'avoir un export automatique. Tant que les demandes sont rares (phase de test), elles sont traitées à la main. Le récapitulatif préparé alors (contenu de l'export, données exclues, bouton avec partage natif `@capacitor/filesystem` + `@capacitor/share` ou envoi par e-mail) reste valable.

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
