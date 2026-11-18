# OPS-13 — CI iOS : certificat de signature fixe et Xcode 27

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| Bêta-test | Haute (signature) / Moyenne (Xcode 27, avant avril 2027) | S | `fix-OPS-13` | À faire |

Origine : échec du workflow TestFlight du 18/11/2026 (run 24) et email App Store Connect du 18/11/2026 sur le build 25.1 (ITMS-90725)

## Problème

**1. Un certificat créé à chaque exécution.** `testflight.yml` signe en mode automatique (`CODE_SIGN_STYLE=Automatic`, `-allowProvisioningUpdates` avec la clé API App Store Connect). Chaque exécution tourne sur un Mac neuf, sans certificat : Xcode en crée un nouveau à l'archive (« Apple Development ») et à l'export (« Apple Distribution »). Apple limite le nombre de certificats par compte ; le run 24 du 18/11/2026 a échoué :

```
error: Choose a certificate to revoke. Your account has reached the maximum number of certificates.
error: No profiles for 'com.loupsolitaire.app' were found
```

Débloqué à la main en révoquant les anciens certificats créés par la CI (run 25 réussi), mais la panne reviendra au bout de quelques exécutions.

**2. SDK iOS trop ancien à partir d'avril 2027.** `runs-on: macos-latest` utilise Xcode 26.6 (SDK iOS 26.5). Email du 18/11/2026 (avertissement, build accepté) :

> ITMS-90725 : Starting April 2027, iOS and iPadOS apps must be built with the iOS 27 SDK or later, included in Xcode 27 or later.

## À faire

**Apple Developer (porteur du projet)**
1. Créer **un seul** certificat « Apple Distribution » (depuis Xcode sur Mac : Settings → Accounts → Manage Certificates, ou par une CSR sur developer.apple.com), l'exporter en `.p12` avec un mot de passe.
2. Créer un profil de provisionnement **App Store** pour `com.loupsolitaire.app` lié à ce certificat, le télécharger (`.mobileprovision`).
3. Secrets GitHub : `IOS_DISTRIBUTION_P12_BASE64`, `IOS_DISTRIBUTION_P12_PASSWORD`, `IOS_PROVISIONING_PROFILE_BASE64` (`base64 -i fichier` sur Mac).
4. Révoquer les certificats « Created via API » restants, une fois le nouveau workflow vert.

**Code (`.github/workflows/testflight.yml`)**
- Étape d'import : trousseau temporaire (`security create-keychain`, `import` du `.p12`, `set-key-partition-list`), profil copié dans `~/Library/MobileDevice/Provisioning Profiles/` ; trousseau et fichiers supprimés en `if: always()` (comme la clé API aujourd'hui).
- Archive en signature **manuelle** : `CODE_SIGN_STYLE=Manual`, `CODE_SIGN_IDENTITY="Apple Distribution"`, `PROVISIONING_PROFILE_SPECIFIER=<nom du profil>` ; plus de `-allowProvisioningUpdates` à l'archive.
- `ExportOptions.plist` : `signingStyle` = `manual`, `provisioningProfiles` → `com.loupsolitaire.app` : nom du profil. L'envoi garde la clé API (`-authenticationKey*`) pour l'upload.
- Xcode 27 : `runs-on` sur une image qui l'embarque et/ou `sudo xcode-select -s /Applications/Xcode_27*.app` ; afficher `xcodebuild -version` dans le log. Vérifier d'abord les versions installées sur les runners GitHub (une exécution avec `ls /Applications | grep Xcode`).
- `frontend/README.md` : procédure de renouvellement (le certificat Apple Distribution expire au bout d'un an, le profil avec lui).

## Critères d'acceptation

- Deux exécutions de suite du workflow TestFlight réussissent **sans créer de certificat** (liste des certificats inchangée sur developer.apple.com).
- Le log affiche Xcode 27 ; le build arrive sur TestFlight sans email ITMS-90725.
- Build installé et lancé sur iPhone via TestFlight.

## Tests

Pas de test automatique (workflow) : les deux exécutions et la vérification sur iPhone ci-dessus.

## Fichiers concernés

`.github/workflows/testflight.yml`, `frontend/README.md`

## À demander avant de commencer

- As-tu un Mac pour créer et exporter le certificat en `.p12` (sinon : CSR créée avec `openssl` sous Linux/WSL, puis conversion en `.p12`) ?
- Faire Xcode 27 dans la même PR, ou attendre que `macos-latest` passe à Xcode 27 de lui-même (échéance avril 2027) ?
