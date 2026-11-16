# REGLE-15 — Retirer 1 coin par retour ou résurrection

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Moyenne | M | `feature-REGLE-15` | À faire (reportée) |

Origine : Cahier des charges (REGLE-15, décision D-07)

## Report (16/11/2026)

Reportée du lot 2 au lot 3 par le porteur du projet : le coin reste non décompté (règle actuelle de `CLAUDE.md`). À reprendre avec QUAL-04 (portefeuille de coins), avec l'affichage des Pièces Premium du profil laissé à « 999 » par QUAL-08.

## Problème

Le coin n'est jamais décompté (`PersonnageService.verifierPossedeCoin` vérifie seulement sa présence). **Attention : contredit la règle actuelle de `CLAUDE.md` (« pas consommée en MVP1, ne pas changer sans demande »). D-07 est cette demande : mettre `CLAUDE.md` à jour dans la même PR.**

## À faire

- `revenirApresDefaite` et `ressusciter` retirent 1 coin (`inventaireService.retirerObjet`) ; refus (400) à 0 coin.
- Stock initial maintenu à 999 pendant la bêta.
- Interdire `DELETE /objets/coin` (sinon on peut le jeter, voir QUAL-04).
- Mettre à jour `CLAUDE.md` (section Règles du jeu).

## Critères d'acceptation

- Chaque retour ou résurrection retire 1 coin ; à 0 coin → 400.

## Tests

`PersonnageServiceTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/PersonnageService.java`, `CLAUDE.md`
