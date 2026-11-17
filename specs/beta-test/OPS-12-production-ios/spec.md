# OPS-12 — Production iOS (App Store)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| Bêta-test | Moyenne | M | `feature-OPS-12` | À faire (après OPS-10) |

Origine : audit du déploiement du 17/11/2026

## Problème

La version iOS n'existe qu'en TestFlight. La publication sur l'App Store demande sa propre fiche et ses propres déclarations, équivalentes à celles faites sur Google Play le 17/11/2026.

## À faire

**App Store Connect (porteur du projet)**
1. Fiche : nom, sous-titre, description (reprendre celle de Google Play), mots-clés, captures iPhone (6,7" et 6,5" au minimum), icône déjà dans l'app.
2. **Confidentialité de l'app** (équivalent « Sécurité des données ») : email et identifiant utilisateur (fonctionnalité, gestion du compte), contenu de jeu (fonctionnalité), données de plantage et de diagnostic (Sentry, analyse) ; aucun suivi (tracking), aucune donnée liée à la publicité.
3. URL de la politique de confidentialité : `https://loup-solitaire-api.onrender.com/api/v1/legal/confidentialite.html`. La suppression du compte dans l'app (Profil → Supprimer mon compte) répond déjà à l'exigence d'Apple.
4. Classification par âge (violence fantastique en texte), catégorie Jeux → Aventure.
5. Informations pour l'examen : compte de test (identifiant et mot de passe, email confirmé), même compte que pour Google Play.
6. Envoyer la build TestFlight validée en examen.

## Critères d'acceptation

- App disponible sur l'App Store (France au minimum).

## Tests

Installation depuis l'App Store sur un iPhone qui n'est pas testeur.

## Fichiers concernés

Aucun (configuration App Store Connect), sauf retour de l'examen.

## À demander avant de commencer

- Autorisation des ayants droit disponible (voir OPS-11) ?
