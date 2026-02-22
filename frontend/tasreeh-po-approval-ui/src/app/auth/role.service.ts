import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class RoleService {
    private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

    constructor(private keycloak: KeycloakService) { }

    getRoles(): string[] {
        if (!this.isBrowser) return [];
        try {
            return this.keycloak.getUserRoles(true);
        } catch {
            return [];
        }
    }

    hasRole(role: string): boolean {
        return this.getRoles().includes(role);
    }

    isManager(): boolean {
        return this.hasRole('manager');
    }

    isEmployee(): boolean {
        return this.hasRole('employee');
    }

    getUserName(): string {
        if (!this.isBrowser) return '';
        try {
            const token = (this.keycloak as any)._instance?.tokenParsed;
            return token?.preferred_username || token?.name || '';
        } catch {
            return '';
        }
    }

    getFullName(): string {
        if (!this.isBrowser) return '';
        try {
            const token = (this.keycloak as any)._instance?.tokenParsed;
            return token?.name || token?.preferred_username || '';
        } catch {
            return '';
        }
    }

    getEmail(): string {
        if (!this.isBrowser) return '';
        try {
            const token = (this.keycloak as any)._instance?.tokenParsed;
            return token?.email || '';
        } catch {
            return '';
        }
    }
}
