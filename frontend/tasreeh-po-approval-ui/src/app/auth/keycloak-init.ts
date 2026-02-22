import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../environments/environment';

export function initializeKeycloak(keycloak: KeycloakService) {
  return () => {
    const platformId = inject(PLATFORM_ID);
    if (!isPlatformBrowser(platformId)) {
      return Promise.resolve();
    }
    return keycloak.init({
      config: {
        url: environment.keycloak.url,
        realm: environment.keycloak.realm,
        clientId: environment.keycloak.clientId
      },
      initOptions: {
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false
      }
    });
  };
}
