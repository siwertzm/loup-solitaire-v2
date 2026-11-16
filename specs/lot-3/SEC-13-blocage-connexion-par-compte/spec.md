# SEC-13 — Blocage après mots de passe faux compté par compte

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 3 | Basse | S | `fix-SEC-13` | À faire |

Origine : second audit Claude Code du 17/11/2026 (point 3)

## Problème

Après 10 mots de passe faux en 15 minutes pour un même identifiant, la connexion répond `429` (`LimiteurDeDebit.LOGIN_COMPTE`, SEC-02). La clé est le texte saisi, mis en minuscules (`AuthService.connecter`) :

- le pseudo et l'email d'un même compte ont chacun leur compteur : 20 essais au lieu de 10 ;
- les pseudos sont sensibles à la casse en base, mais les compteurs non : des essais faux sur `Bob` bloquent aussi `bob`.

## À faire

Retrouver le compte comme `CustomUserDetailsService` (email prioritaire si l'identifiant contient `@`). S'il existe, utiliser son id comme clé ; sinon garder le texte saisi, pour que la réponse ne révèle pas si le compte existe.

## Critères d'acceptation

- 5 échecs par pseudo + 5 par email du même compte → `429` au 11e essai, quelle que soit la forme.
- Les échecs sur un pseudo ne bloquent pas un autre compte dont le pseudo ne diffère que par la casse.
- Identifiant inconnu : même réponse et même durée qu'avant (SEC-05, SEC-11).

## Tests

`AuthServiceTest` : compteur partagé pseudo/email ; comptes `Bob`/`bob` séparés.

## Fichiers concernés

`AuthService.java`
