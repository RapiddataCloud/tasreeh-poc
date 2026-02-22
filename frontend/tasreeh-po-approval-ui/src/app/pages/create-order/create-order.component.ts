import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { OrderApiService } from '../../services/order-api.service';
import { IndexedDbService } from '../../services/indexeddb.service';

@Component({
  selector: 'app-create-order',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './create-order.component.html',
  styleUrl: './create-order.component.scss'
})
export class CreateOrderComponent {
  item = '';
  amount: number | null = null;
  description = '';
  submitting = false;
  errorMsg = '';

  constructor(
    private api: OrderApiService,
    private router: Router,
    private idb: IndexedDbService
  ) { }

  async submit() {
    this.errorMsg = '';
    if (!this.item || this.amount === null || this.amount <= 0) {
      this.errorMsg = 'Please enter Item and valid Amount.';
      return;
    }

    this.submitting = true;
    this.api.createOrder({ item: this.item, amount: this.amount, description: this.description }).subscribe({
      next: async (createdOrder) => {
        console.log('[CreateOrder] ✅ Order created:', createdOrder);
        // Store new order in IndexedDB so dashboard shows it instantly
        await this.idb.open();
        await this.idb.addOrder(createdOrder);
        this.router.navigateByUrl('/employee/dashboard');
      },
      error: (err) => {
        console.error('[CreateOrder] ❌ Failed:', err);
        this.errorMsg = 'Failed to create request. Please try again.';
        this.submitting = false;
      }
    });
  }
}
