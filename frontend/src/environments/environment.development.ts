export const environment = {
  production: false,
  // QUAL-02 : API versionnée (les anciens chemins restent servis pour les apps déjà installées).
  apiUrl: 'http://localhost:8080/api/v1',
  // OPS-04 : DSN du projet Sentry de l'app (public par nature, il ne permet
  // que d'envoyer des erreurs). Vide = suivi des erreurs désactivé.
  sentryDsn: '',
  sentryEnvironment: 'development',
};
