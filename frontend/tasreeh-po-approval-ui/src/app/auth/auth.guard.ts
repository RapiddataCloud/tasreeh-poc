import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';

export const authGuard: CanActivateFn = async () => {
    const platformId = inject(PLATFORM_ID);
    if (!isPlatformBrowser(platformId)) {
        return true; // Allow SSR pass-through
    }

    const keycloak = inject(KeycloakService);
    const router = inject(Router);

    try {
        const loggedIn = await keycloak.isLoggedIn();
        if (loggedIn) {
            return true;
        }
    } catch { }

    return router.createUrlTree(['/login']);
};
