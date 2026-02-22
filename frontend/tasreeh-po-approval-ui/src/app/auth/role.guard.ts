import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RoleService } from './role.service';

export function roleGuard(requiredRole: string): CanActivateFn {
    return async () => {
        const platformId = inject(PLATFORM_ID);
        if (!isPlatformBrowser(platformId)) return true;

        const roleService = inject(RoleService);
        const router = inject(Router);

        if (roleService.hasRole(requiredRole)) {
            return true;
        }

        // Redirect to the appropriate dashboard based on actual role
        if (roleService.isManager()) {
            return router.createUrlTree(['/manager/dashboard']);
        }
        return router.createUrlTree(['/employee/dashboard']);
    };
}
