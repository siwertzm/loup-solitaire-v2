# OPS-10 — Version iOS : reprise de TestFlight

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| Bêta-test | Haute | S | `fix-OPS-10` | Fait |

Origine : audit du déploiement du 17/11/2026

## Problème

- Le workflow `testflight.yml` a réussi le 16/11/2026, puis échoue depuis le 17/11/2026 à l'export : `exportArchive Error Downloading App Information`. Erreur côté compte Apple (adhésion ou accords App Store Connect à renouveler / accepter), en attente du porteur du projet.
- Plusieurs changements du 17/11/2026 n'ont jamais tourné sur un iPhone :
  - QUAL-22 : nouveau splash iOS (loup sur `#2c3323`) ;
  - QUAL-23 : `MARKETING_VERSION` lu dans `frontend/package.json` (1.0.0) par le workflow ;
  - QUAL-24/25 : règles de marge du bas propres à Android, iOS censé être inchangé ;
  - SEC-14 : le lien de vérification d'email (`loupsolitaire://auth/login`) était déjà déclaré sur iOS, à revérifier.
- `Info.plist` ne déclare pas `ITSAppUsesNonExemptEncryption` : App Store Connect pose la question du chiffrement à chaque build (l'app n'utilise que HTTPS : exemptée).

## À faire

1. **Porteur du projet** : régulariser le compte Apple Developer (adhésion, accords et contrats dans App Store Connect).
2. Relancer `testflight.yml` ; si l'erreur persiste, lire le log de l'étape « Upload to TestFlight ».
3. `Info.plist` : `ITSAppUsesNonExemptEncryption` = `false`.
4. Vérifier qu'un manifeste de confidentialité (`PrivacyInfo.xcprivacy`) couvre l'usage de `UserDefaults` (`@capacitor/preferences`) ; l'ajouter à l'app si Apple le réclame à l'envoi.

## Critères d'acceptation

- Le workflow TestFlight réussit ; la build apparaît dans App Store Connect en version **1.0.0**, sans question sur le chiffrement.
- Sur iPhone : icône et splash du loup ; lien de vérification d'email qui ouvre l'app sur la connexion ; bas des écrans comme avant (barre d'onglets, Fiche, combat, feuille d'inventaire) ; partie complète jouable.

## Tests

Parcours manuel sur iPhone (critères ci-dessus).

## Fichiers concernés

`frontend/ios/App/App/Info.plist`, éventuellement `frontend/ios/App/App/PrivacyInfo.xcprivacy`, `.github/workflows/testflight.yml`

## À demander avant de commencer

- Le compte Apple Developer est-il régularisé ?

## Fait (18/11/2026)

- Compte Apple Developer régularisé ; `testflight.yml` réussi le 18/11/2026, build en version 1.0.0.
- `Info.plist` : `ITSAppUsesNonExemptEncryption` = `false`.
- `PrivacyInfo.xcprivacy` non ajouté : envoi accepté sans avertissement d'Apple (à ajouter si un email ITMS-91053 arrive).
- Vérifié sur iPhone par le porteur du projet : icône et splash, lien de vérification d'email, bas des écrans, partie jouable.
