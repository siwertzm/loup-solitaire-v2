# SEC-05 — Réponses identiques à l'inscription et au mot de passe oublié

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-SEC-05` | À faire |

Origine : Cahier des charges (SEC-05)

## Problème

`CompteService.inscrire` répond 409 « Cet email est deja utilise » : on peut tester si un email a un compte. L'envoi des emails est déjà asynchrone (OPS-02), il reste la différence de réponse.

## À faire

- Inscription : même réponse dans tous les cas ; si l'email existe déjà, envoyer à cette adresse un email « vous avez déjà un compte ».
- Garder le 409 pour un **nom d'utilisateur** pris (il est public dans le jeu) ou le traiter de la même façon — à décider.
- Mettre à jour la page d'inscription (`fr.json`).

## Critères d'acceptation

- Corps et code de réponse identiques pour un email connu et un email inconnu.

## Tests

`AuthControllerTest`.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/CompteService.java`, `EmailService.java`, page register
