# SEC-06 — Mot de passe exigé pour changer d'email ; ancien email actif jusqu'à confirmation

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| 2 | Moyenne | M | `feature-SEC-06` | À faire |

Origine : Cahier des charges (SEC-06) + audit Claude Code du 11/11/2026

## Problème

`CompteService.modifierProfil` change l'email sans demander le mot de passe et passe tout de suite `emailVerifie` à `false`. Or `AuthService.connecter` refuse tout compte non vérifié.

- **Faute de frappe** : le lien part vers une adresse fausse ; après la prochaine déconnexion, le joueur ne peut plus se connecter, et le renvoi du lien part encore vers la mauvaise adresse.
- **Vol de compte** : un téléphone déverrouillé (ou un access token volé, 15 min) suffit pour mettre son propre email, puis faire « mot de passe oublié ».

## À faire

- Ajouter `motDePasseActuel` dans `UpdateProfilRequest` (+ modèle TS dans `core/models/`), exigé seulement si l'email change ; vérifier avec `passwordEncoder.matches` (même chose que `changerMotDePasse`).
- Nouvelle colonne `email_en_attente` (migration Liquibase avec `rollback`) : le nouvel email n'y est stocké qu'en attente ; l'email actuel et `emailVerifie` ne changent pas.
- Au clic sur le lien de vérification : `email = email_en_attente`, `email_en_attente = null`.
- Prévenir l'ancienne adresse par email.
- Écran profil : champ mot de passe quand l'email change ; textes dans `fr.json`.

## Critères d'acceptation

- Changer d'email sans mot de passe ou avec un mauvais mot de passe → 401.
- Tant que le lien n'est pas confirmé, la connexion avec l'ancien email fonctionne.
- Après confirmation, seul le nouvel email permet de se connecter.

## Tests

`CompteServiceTest` : sans mot de passe → 401 ; email en attente ne bloque pas la connexion ; confirmation → email remplacé.

## Fichiers concernés

`backend/src/main/java/com/loupsolitaire/backend/service/CompteService.java`, `EmailVerificationService.java`, `request/UpdateProfilRequest.java`, `model/Utilisateur.java`, migration `015-email-en-attente`, pages profil du frontend
