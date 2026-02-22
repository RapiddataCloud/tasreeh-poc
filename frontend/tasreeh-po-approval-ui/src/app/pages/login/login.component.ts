import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { RoleService } from '../../auth/role.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './login.component.html',
    styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
    private platformId = inject(PLATFORM_ID);
    private isBrowser = isPlatformBrowser(this.platformId);
    loading = false;

    constructor(
        private keycloak: KeycloakService,
        private router: Router,
        private roleService: RoleService
    ) { }

    async ngOnInit() {
        if (!this.isBrowser) return;
        try {
            const loggedIn = await this.keycloak.isLoggedIn();
            if (loggedIn) {
                this.redirectByRole();
            }
        } catch { }
    }

    async login() {
        if (!this.isBrowser) return;
        this.loading = true;
        try {
            await this.keycloak.login({ redirectUri: window.location.origin + '/login' });
        } catch {
            this.loading = false;
        }
    }

    private redirectByRole() {
        if (this.roleService.isManager()) {
            this.router.navigate(['/manager/dashboard']);
        } else {
            this.router.navigate(['/employee/dashboard']);
        }
    }
}
