import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { CreateOrderComponent } from './pages/create-order/create-order.component';
import { ManagerDashboardComponent } from './pages/manager-dashboard/manager-dashboard.component';
import { PendingApprovalsComponent } from './pages/pending-approvals/pending-approvals.component';
import { AllOrdersComponent } from './pages/all-orders/all-orders.component';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  // Employee routes
  {
    path: 'employee',
    component: LayoutComponent,
    canActivate: [authGuard, roleGuard('employee')],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'create', component: CreateOrderComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // Manager routes
  {
    path: 'manager',
    component: LayoutComponent,
    canActivate: [authGuard, roleGuard('manager')],
    children: [
      { path: 'dashboard', component: ManagerDashboardComponent },
      { path: 'pending', component: PendingApprovalsComponent },
      { path: 'orders', component: AllOrdersComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
