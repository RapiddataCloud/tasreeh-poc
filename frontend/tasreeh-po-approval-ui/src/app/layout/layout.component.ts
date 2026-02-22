import { Component, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterModule } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { RoleService } from '../auth/role.service';

@Component({
    selector: 'app-layout',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './layout.component.html',
    styleUrl: './layout.component.scss'
})
export class LayoutComponent {
    private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
    sidebarCollapsed = false;
    mobileSidebarOpen = false;

    constructor(public roleService: RoleService, private keycloak: KeycloakService) { }

    get isManager(): boolean {
        return this.roleService.isManager();
    }

    get isEmployee(): boolean {
        return this.roleService.isEmployee();
    }

    get userName(): string {
        return this.roleService.getFullName();
    }

    get userInitials(): string {
        const name = this.userName;
        if (!name) return 'U';
        const parts = name.split(' ');
        return parts.map(p => p[0]).join('').toUpperCase().slice(0, 2);
    }

    get userRole(): string {
        if (this.isManager) return 'Manager';
        if (this.isEmployee) return 'Employee';
        return 'User';
    }

    toggleSidebar() {
        this.sidebarCollapsed = !this.sidebarCollapsed;
    }

    toggleMobileSidebar() {
        this.mobileSidebarOpen = !this.mobileSidebarOpen;
    }

    async logout() {
        if (!this.isBrowser) return;
        await this.keycloak.logout(window.location.origin + '/login');
    }
}
