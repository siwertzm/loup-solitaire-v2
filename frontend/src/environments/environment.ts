export const environment = {
  production: true,
  // QUAL-02 : API versionnée (les anciens chemins restent servis pour les apps déjà installées).
  apiUrl: 'https://loup-solitaire-api.onrender.com/api/v1',
  // OPS-04 : DSN du projet Sentry de l'app (public par nature, il ne permet
  // que d'envoyer des erreurs). Vide = suivi des erreurs désactivé.
  sentryDsn: 'https://7b4c320bdb5be36f04263fa96840279b@o4512150622633984.ingest.de.sentry.io/4512150668378192',
  sentryEnvironment: 'production',
};
