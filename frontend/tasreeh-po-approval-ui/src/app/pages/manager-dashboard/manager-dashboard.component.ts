import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderApiService } from '../../services/order-api.service';
import { PurchaseOrder } from '../../models/purchase-order.model';

@Component({
    selector: 'app-manager-dashboard',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './manager-dashboard.component.html',
    styleUrl: './manager-dashboard.component.scss'
})
export class ManagerDashboardComponent implements OnInit {
    orders: PurchaseOrder[] = [];
    loading = true;
    activeTab: 'all' | 'pending' | 'auto_approved' | 'manual_approved' = 'all';

    constructor(private api: OrderApiService, private cdr: ChangeDetectorRef) { }

    ngOnInit() {
        this.loadOrders();
    }

    loadOrders() {
        this.loading = true;
        this.cdr.detectChanges();
        this.api.getAllOrders().subscribe({
            next: (data) => {
                this.orders = data;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    get stats() {
        const total = this.orders.length;
        const pending = this.orders.filter(o => this.isPending(o.status)).length;
        const autoApproved = this.orders.filter(o => o.status === 'AUTO_APPROVED').length;
        const approved = this.orders.filter(o => o.status === 'APPROVED').length;
        const rejected = this.orders.filter(o => this.isRejected(o.status)).length;
        return { total, pending, autoApproved, manualApproved: approved, approved: autoApproved + approved, rejected };
    }

    get filteredOrders(): PurchaseOrder[] {
        switch (this.activeTab) {
            case 'pending': return this.orders.filter(o => this.isPending(o.status));
            case 'auto_approved': return this.orders.filter(o => o.status === 'AUTO_APPROVED');
            case 'manual_approved': return this.orders.filter(o => o.status === 'APPROVED');
            default: return this.orders;
        }
    }

    setTab(tab: typeof this.activeTab) {
        this.activeTab = tab;
    }

    isPending(status: string): boolean {
        return ['PENDING', 'PENDING_APPROVAL'].includes(status);
    }

    isApproved(status: string): boolean {
        return ['APPROVED', 'AUTO_APPROVED'].includes(status);
    }

    isRejected(status: string): boolean {
        return ['REJECTED'].includes(status);
    }
}
