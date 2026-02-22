export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8090/api', // Nginx → KrakenD
  wsUrl: 'ws://localhost:8090/ws',         // Nginx → notification-service
  keycloak: {
    url: 'http://localhost:8081',
    realm: 'tasreeh-po-realm',
    clientId: 'angular-spa'
  }
};
