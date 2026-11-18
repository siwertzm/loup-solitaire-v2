# OPS-09 — Test fermé Android (Google Play)

| Lot | Sévérité | Effort | Branche | Statut |
| --- | --- | --- | --- | --- |
| Bêta-test | Haute | S (code) + 14 jours (Play) | `feature-OPS-09` | En cours (14 jours : en attente des inscriptions) |

Origine : audit du déploiement Android du 17/11/2026

## Problème

L'app est en **test interne** sur Google Play (builds 107 à 113, envoi automatique par la CI depuis le 17/11/2026). Mais :

- Google n'examine jamais le test interne : la fiche Play Store reste « provisoire » pour les testeurs (icône grise, nom du package `com.loupsolitaire.app`) et rien ne part en examen. La Vue d'ensemble de la publication indique « Pour envoyer des modifications pour examen, suivez les étapes requises dans le tableau de bord ».
- Un compte développeur **personnel** créé après le 13/11/2023 doit faire un **test fermé avec au moins 12 testeurs inscrits pendant 14 jours d'affilée** avant de pouvoir demander l'accès à la production (règle relevée sur des sources tierces, à confirmer dans le tableau de bord de la console).
- Le workflow `android-internal-test.yml` envoie toujours sur la piste `internal` (en dur).

## À faire

**Play Console (porteur du projet)**
1. Tester et publier → Tests → **Test fermé** : piste par défaut (« Closed testing », `alpha` côté API), pays **France** au minimum.
2. Liste de testeurs : au moins 12 adresses Gmail (amis, famille, joueurs), porteur du projet compris.
3. Créer une release : **Ajouter depuis la bibliothèque** le dernier build (113 ou plus), mêmes notes de version.
4. Vue d'ensemble de la publication → **Envoyer les modifications pour examen** (fiche, formulaires et release partent ensemble au premier examen).
5. Après validation : envoyer le lien d'inscription aux testeurs ; chacun s'inscrit **et installe** l'app (Google vérifie l'usage réel). Les 14 jours démarrent quand les 12 testeurs sont inscrits.
6. Noter dans ce spec la date de début des 14 jours.

**Code**
- `android-internal-test.yml` : entrée `track` (`internal` par défaut, `alpha` pour le test fermé) passée à `r0adkll/upload-google-play`. Renommer le workflow si besoin (« Android Play »). Le compte de service `github-ci-play` a déjà les droits sur les canaux de test.

## Critères d'acceptation

- Premier examen Google validé ; la fiche s'affiche (icône, captures, description) sur le téléphone d'un testeur.
- 12 testeurs inscrits au test fermé, dont au moins quelques-uns qui jouent réellement.
- Le workflow envoie sur `internal` ou `alpha` selon l'entrée choisie ; `internal` reste le défaut.

## Tests

- Lancer le workflow avec `track=alpha` : la release apparaît dans Test fermé.
- Lancer avec la valeur par défaut : comportement inchangé (test interne).

## Fichiers concernés

`.github/workflows/android-internal-test.yml`, `frontend/README.md` (procédure de release)

## À demander avant de commencer

- Le tableau de bord affiche-t-il bien l'exigence « 12 testeurs / 14 jours » pour ce compte ?
- Liste des 12 testeurs prête ?

## Avancement (18/11/2026)

- Play Console : étapes 1 à 5 faites, lien d'inscription envoyé aux testeurs.
- Code : entrée `track` du workflow (`internal` par défaut, `alpha` pour le test fermé).
- Début des 14 jours : **en attente des inscriptions** (à noter quand les 12 testeurs sont inscrits).

## Déjà en place (17/11/2026)

Compte Play Console vérifié, app créée, Play App Signing (clé Google), fiche Play Store, formulaires « Contenu de l'application » (sécurité des données, classification, public 13+, accès avec compte de test, suppression de compte), Google Play Games sur PC désactivé, envoi automatique en test interne par la CI.
