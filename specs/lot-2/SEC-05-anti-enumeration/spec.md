# SEC-05 — Réponses identiques à l'inscription et au mot de passe oublié

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | S | `fix-SEC-05` | Fait |

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

## Décisions (12/11/2026)

- `POST /auth/register` : `202` sans contenu dans tous les cas ; mot de passe haché même si l'email existe (même durée de réponse).
- Email déjà inscrit : rappel « tu as déjà un compte » (compte vérifié) ou nouveau lien de vérification (compte non vérifié), au plus 3 par heure et par adresse ; au-delà, rien ne part et la réponse reste `202`.
- Nom d'utilisateur pris : `409` gardé (le joueur doit en choisir un autre ; le nom ne révèle pas l'email).
- Hors périmètre : deux inscriptions simultanées avec le même email (la seconde reçoit le `409` de la base, SEC-08) ; changement d'email dans le profil (`409`, nécessite d'être connecté et le mot de passe).

