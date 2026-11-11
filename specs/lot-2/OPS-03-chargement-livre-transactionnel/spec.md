# OPS-03 — Chargement du livre transactionnel + contrôle d'intégrité

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-OPS-03` | À faire |

Origine : Cahier des charges (OPS-03)

## Problème

`GameDataLoader` saute chaque étape dès que la table correspondante n'est pas vide (`count() > 0`) et n'est pas transactionnel. Un crash entre deux passes laisse par exemple des chapitres sans liens, définitivement.

## À faire

- Chargement complet dans une seule transaction.
- Contrôle d'intégrité au démarrage : 353 chapitres, aucun chapitre sans lien sauf le 350 ; échec du démarrage sinon.
- Voir aussi DATA-00, qui peut remplacer ce mécanisme.

## Critères d'acceptation

- Crash simulé en passe 2 → base vide, rechargement complet au démarrage suivant.

## Tests

`GameDataLoaderTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/config/GameDataLoader.java`
