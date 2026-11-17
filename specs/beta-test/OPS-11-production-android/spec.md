# OPS-11 — Production Android (Google Play)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| Bêta-test | Moyenne | S | `feature-OPS-11` | À faire (après OPS-09) |

Origine : audit du déploiement Android du 17/11/2026

## Problème

Après le test fermé (OPS-09), l'app doit passer en production pour être visible de tous sur le Play Store.

## À faire

1. Après les 14 jours : **Demander l'accès à la production** depuis le tableau de bord (questionnaire sur le test fermé : nombre de testeurs, retours, corrections faites). Examen annoncé en 7 jours environ.
2. Créer la release de production depuis la bibliothèque ; déploiement progressif conseillé (par exemple 20 %, puis 100 %).
3. Workflow : garder la production **manuelle** dans la console (le compte de service n'a volontairement que les droits sur les canaux de test).
4. Propriété intellectuelle : « Loup Solitaire / Lone Wolf » et le texte du livre appartiennent à leurs ayants droit ; garder sous la main la preuve d'autorisation, Google peut la demander à l'examen.

## Critères d'acceptation

- App disponible en production sur le Play Store (France au minimum).
- Procédure de release à jour dans `frontend/README.md` (test interne → test fermé → production, puis `APP_VERSION_MINIMALE` si la release force la mise à jour, QUAL-23).

## Tests

Installation depuis la fiche publique sur un téléphone qui n'est pas testeur.

## Fichiers concernés

`frontend/README.md`

## À demander avant de commencer

- Autorisation des ayants droit disponible ?
- Déploiement progressif ou direct à 100 % ?
