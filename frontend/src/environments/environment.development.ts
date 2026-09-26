export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  // OPS-04 : DSN du projet Sentry de l'app (public par nature, il ne permet
  // que d'envoyer des erreurs). Vide = suivi des erreurs désactivé.
  sentryDsn: '',
  sentryEnvironment: 'development',
};
