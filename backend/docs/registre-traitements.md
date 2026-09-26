# Registre des traitements et sous-traitants (RGPD-04)

Document interne (article 30 du RGPD), à ne pas publier. À tenir à jour à
chaque nouvelle donnée, nouveau prestataire ou changement d'offre. La
version publique pour les joueurs est
`src/main/resources/static/legal/confidentialite.html`.

Dernière mise à jour : 26/09/2026.

## Responsable du traitement

- Identité : Marius Siwertz, particulier (adresse postale conservée hors du
  dépôt, qui est public)
- Contact : contact@lone-wolf.fr
- Délégué à la protection des données : aucun (non obligatoire).

## Traitements

| Traitement | Finalité | Base légale | Personnes | Données | Destinataires | Conservation |
|---|---|---|---|---|---|---|
| Comptes | Créer et gérer le compte, authentifier | Contrat (CGU) | Joueurs | Nom d'utilisateur, email, hash BCrypt du mot de passe, date de création, email vérifié | Render (hébergement) | Jusqu'à la suppression du compte par le joueur (suppression immédiate et complète : `CompteService.supprimerCompte`) |
| Parties | Faire fonctionner le jeu, sauvegarder la progression | Contrat | Joueurs | Personnages, caractéristiques, disciplines, inventaire, chapitres parcourus, étapes mortelles, combats, tirages de création | Render | Idem compte |
| Sécurité des comptes | Sessions, vérification d'email, réinitialisation, limitation des abus | Intérêt légitime (sécurité) | Joueurs, visiteurs | Refresh tokens (hash SHA-256), tokens de vérification (hash), codes de réinitialisation (hash BCrypt), compteurs par IP et par email | Render | Sessions : 30 jours de validité ; lien de vérification 24 h ; code 15 min ; compteurs en mémoire 24 h au plus. Les lignes expirées restent en base jusqu'à la suppression du compte (purge prévue par RGPD-03 / OPS-05). |
| Emails transactionnels | Confirmation d'adresse, code de réinitialisation | Contrat | Joueurs | Email, contenu du message | Brevo | Selon Brevo (journaux d'envoi) |
| Suivi des erreurs | Détecter et corriger les pannes | Intérêt légitime | Joueurs | Données techniques (erreur, appareil, version, écran) ; configuré sans utilisateur, IP, cookies, en-têtes ni corps de requête (`sentry.data-collection.user-info=false`, `dataCollection` dans `main.ts`) | Sentry | 30 jours (offre Developer ; 90 jours si passage en Team) |
| Journaux serveur | Fonctionnement et sécurité | Intérêt légitime | Joueurs, visiteurs | IP, date, URL | Render | 7 jours (Hobby), 14 (Pro), 30 (Scale) |
| Avis et bugs | Recueillir les retours | Consentement (formulaire facultatif) | Joueurs volontaires | Ce que le joueur écrit | Google (Forms) | À définir ; supprimer les réponses une fois traitées |

Aucune donnée sensible, aucune prospection, aucun profilage, aucune
publicité, aucun traceur soumis à consentement (seulement du stockage
local nécessaire : jetons de connexion, préférences de jeu).

## Sous-traitants

| Prestataire | Rôle | Lieu des données | Accord de sous-traitance (DPA) | Action |
|---|---|---|---|---|
| Render Services, Inc. (USA) | Hébergement API et PostgreSQL | Région du service : Frankfurt = UE | [render.com/dpa](https://render.com/dpa) : intégré aux conditions d'utilisation, clauses contractuelles types réputées signées ; Render est certifié Data Privacy Framework. Sous-traitants : [render.com/trust](https://render.com/trust) | Copie : `Data Processing Addendum _ Render.pdf` |
| Brevo / Sendinblue SAS (France) | Envoi des emails | UE ; transferts possibles vers ses sous-traitants (dont USA, Inde) | Appendix 3 des [conditions d'utilisation Brevo](https://www.brevo.com/legal/termsofuse/), intégré automatiquement (copie : `Brevo Terms of Service.pdf`) | Vérifier que le compte est rattaché à l'entité française |
| Sentry / Functional Software, Inc. (USA) | Rapports d'erreurs | UE (Allemagne, `ingest.de.sentry.io`) | [sentry.io/legal/dpa](https://sentry.io/legal/dpa/) : à **accepter** dans Sentry → Settings → Legal & Compliance (rôle Owner ou Billing) | Accepter le DPA ; vérifier que le projet backend est dans la même organisation (région UE) |
| UptimeRobot | Sonde de disponibilité | — | Non nécessaire : n'appelle que `/actuator/health`, aucune donnée de joueur | Aucune |
| Google (Forms) | Formulaire d'avis | USA / UE | Conditions Google | Ne pas demander d'email ni de nom dans le formulaire |

## Mesures de sécurité

HTTPS ; mots de passe BCrypt ; jetons et codes hachés ; secrets uniquement
en variables d'environnement, démarrage refusé sans eux (SEC-03) ;
limitation de débit sur `/auth/**` (SEC-02) ; révocation de toutes les
sessions en cas de réutilisation d'un refresh token (SEC-04) ; sauvegardes
Render (OPS-06). Reste à faire : masquer l'email dans les journaux
d'erreur (RGPD-03).

## Demandes des joueurs

| Droit | Comment y répondre |
|---|---|
| Accès, portabilité | Export JSON du compte et des personnages à la main (pas encore d'export automatique). Délai : 1 mois. |
| Rectification | Le joueur modifie lui-même nom d'utilisateur et email dans le profil |
| Effacement | Bouton « Supprimer mon compte » ; sinon suppression à la main sur demande |
| Opposition, limitation | Au cas par cas, par email |

Noter chaque demande (date, droit, réponse, date de réponse) :

| Date | Demande | Réponse | Date de réponse |
|---|---|---|---|
| | | | |

## Violation de données

En cas de fuite (base exposée, secret publié...) : noter les faits ici,
notifier la CNIL sous 72 h si un risque existe pour les joueurs
([notifications.cnil.fr](https://notifications.cnil.fr/notifications/index)),
prévenir les joueurs si le risque est élevé, révoquer les sessions et
changer les secrets.

## À faire avant l'ouverture au public

- [x] Compléter l'identité et le contact (ce fichier, `confidentialite.html`, `cgu.html`)
- [x] Compléter la région Render (ce fichier et `confidentialite.html`)
- [ ] Compléter la section « Propriété intellectuelle » des CGU une fois les droits acquis
- [ ] Accepter le DPA Sentry
- [x] Archiver les DPA Render et Brevo
- [ ] Renseigner l'URL de `confidentialite.html` dans les fiches Google Play et App Store
- [ ] Mettre à jour la politique quand RGPD-03 / OPS-05 (purge) seront faits