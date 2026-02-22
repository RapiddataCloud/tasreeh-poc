import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderApiService } from '../../services/order-api.service';
import { PurchaseOrder } from '../../models/purchase-order.model';

@Component({
    selector: 'app-all-orders',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './all-orders.component.html',
    styleUrl: './all-orders.component.scss'
})
export class AllOrdersComponent implements OnInit {
    orders: PurchaseOrder[] = [];
    loading = true;
    statusFilter = 'all';

    constructor(private api: OrderApiService) { }

    ngOnInit() {
        this.api.getAllOrders().subscribe({
            next: (data) => {
                this.orders = data;
                this.loading = false;
            },
            error: () => (this.loading = false)
        });
    }

    get filteredOrders(): PurchaseOrder[] {
        if (this.statusFilter === 'all') return this.orders;
        return this.orders.filter(o => o.status?.toLowerCase() === this.statusFilter);
    }

    get statusOptions(): string[] {
        const statuses = [...new Set(this.orders.map(o => o.status?.toLowerCase()).filter(Boolean))];
        return statuses.sort();
    }
}
