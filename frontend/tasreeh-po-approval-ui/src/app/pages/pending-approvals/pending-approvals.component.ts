import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderApiService } from '../../services/order-api.service';
import { PurchaseOrder } from '../../models/purchase-order.model';

@Component({
    selector: 'app-pending-approvals',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './pending-approvals.component.html',
    styleUrl: './pending-approvals.component.scss'
})
export class PendingApprovalsComponent implements OnInit {
    orders: PurchaseOrder[] = [];
    loading = true;
    actionLoading: Record<string, boolean> = {};
    rejectReasons: Record<string, string> = {};
    showRejectModal: string | null = null;

    constructor(private api: OrderApiService, private cdr: ChangeDetectorRef) { }

    ngOnInit() {
        this.loadPending();
    }

    loadPending() {
        this.loading = true;
        this.cdr.detectChanges();
        this.api.getPendingOrders().subscribe({
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

    approve(order: PurchaseOrder) {
        this.actionLoading[order.id] = true;
        this.cdr.detectChanges();
        this.api.approveOrder(order.id).subscribe({
            next: () => {
                // Remove from pending list — status is now APPROVED
                this.orders = this.orders.filter(o => o.id !== order.id);
                delete this.actionLoading[order.id];
                this.cdr.detectChanges();
            },
            error: (err) => {
                console.error('Approve failed:', err);
                delete this.actionLoading[order.id];
                this.cdr.detectChanges();
            }
        });
    }

    openRejectModal(orderId: string) {
        this.showRejectModal = orderId;
        this.rejectReasons[orderId] = '';
        this.cdr.detectChanges();
    }

    closeRejectModal() {
        this.showRejectModal = null;
        this.cdr.detectChanges();
    }

    confirmReject() {
        if (!this.showRejectModal) return;
        const id = this.showRejectModal;
        const reason = this.rejectReasons[id] || 'Rejected by manager';
        this.actionLoading[id] = true;
        this.closeRejectModal();

        this.api.rejectOrder(id, reason).subscribe({
            next: () => {
                // Remove from pending list — status is now REJECTED
                this.orders = this.orders.filter(o => o.id !== id);
                delete this.actionLoading[id];
                this.cdr.detectChanges();
            },
            error: (err) => {
                console.error('Reject failed:', err);
                delete this.actionLoading[id];
                this.cdr.detectChanges();
            }
        });
    }
}
