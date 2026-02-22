import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { OrderApiService } from '../../services/order-api.service';
import { RealtimeService, StatusUpdate } from '../../services/realtime.service';
import { IndexedDbService } from '../../services/indexeddb.service';
import { PurchaseOrder } from '../../models/purchase-order.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  orders: PurchaseOrder[] = [];
  loading = true;

  private sub = new Subscription();
  /** Tracks WS status updates that arrived before the API response */
  private pendingUpdates: StatusUpdate[] = [];
  /** All WS updates received during this session — replayed after each data load */
  private allWsUpdates: StatusUpdate[] = [];
  private ordersLoaded = false;

  constructor(
    private api: OrderApiService,
    private realtime: RealtimeService,
    private idb: IndexedDbService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.setupWebSocket();
    this.loadData();
  }

  private async loadData() {
    try {
      // 1. Open IndexedDB
      await this.idb.open();

      // 2. Load from IndexedDB cache first (instant render)
      const cached = await this.idb.getOrders();
      if (cached.length > 0) {
        console.log('[Employee Dashboard] Loaded', cached.length, 'orders from IndexedDB cache');
        this.orders = cached;
        this.loading = false;
        this.applyPendingUpdates();
        this.cdr.detectChanges();
      }
    } catch (e) {
      console.warn('[Employee Dashboard] IndexedDB not available:', e);
    }

    // 3. Fetch fresh data from API and update IndexedDB
    this.sub.add(
      this.api.getMyOrders().subscribe({
        next: async (data) => {
          console.log('[Employee Dashboard] Loaded', data.length, 'orders from API');
          this.orders = data;
          this.ordersLoaded = true;
          this.loading = false;
          // Re-apply ALL WebSocket updates received during this session,
          // because the API data may be stale compared to WS notifications
          this.reapplyAllWsUpdates();
          this.cdr.detectChanges();
          // Store in IndexedDB for next visit (with WS-corrected statuses)
          try { await this.idb.storeOrders(this.orders); } catch (e) { /* ignore */ }
        },
        error: (err) => {
          console.error('[Employee Dashboard] API error:', err);
          this.ordersLoaded = true;
          this.loading = false;
          this.applyPendingUpdates();
          this.cdr.detectChanges();
        }
      })
    );
  }

  /** Apply any WebSocket updates that arrived before orders were loaded */
  private applyPendingUpdates() {
    if (this.pendingUpdates.length === 0) return;
    console.log('[Employee Dashboard] Applying', this.pendingUpdates.length, 'pending WebSocket updates');
    for (const u of this.pendingUpdates) {
      this.applyStatusUpdate(u);
    }
    this.pendingUpdates = [];
  }

  /**
   * Re-apply ALL WebSocket updates received during this session.
   * Called after the API response overwrites this.orders, because the API
   * data may be stale (e.g. still showing CREATED when WS already sent PENDING_APPROVAL).
   */
  private reapplyAllWsUpdates() {
    // First apply any still-pending updates
    this.applyPendingUpdates();
    // Then re-apply all WS updates we've ever received this session
    if (this.allWsUpdates.length > 0) {
      console.log('[Employee Dashboard] Re-applying', this.allWsUpdates.length, 'WS updates over API data');
      for (const u of this.allWsUpdates) {
        this.applyStatusUpdate(u);
      }
    }
  }

  /** Apply a single status update to the orders array */
  private applyStatusUpdate(u: StatusUpdate): boolean {
    const idx = this.orders.findIndex((o) => o.id === u.orderId);
    if (idx >= 0) {
      this.orders = this.orders.map((o, i) =>
        i === idx ? { ...o, status: u.newStatus } : o
      );
      // Also update IndexedDB cache
      this.idb.updateOrderStatus(u.orderId, u.newStatus).catch(() => { /* ignore */ });
      console.log('[Employee Dashboard] => Table row updated for', u.orderId);
      return true;
    }
    return false;
  }

  private async setupWebSocket() {
    // 4. Connect WebSocket for real-time status updates (employee only)
    await this.realtime.connect();

    console.log('========================================================');
    console.log('[Employee Dashboard] 🔌 WebSocket connected, waiting for status updates...');
    console.log('[Employee Dashboard] Status changes will update table WITHOUT API calls');
    console.log('========================================================');

    this.sub.add(
      this.realtime.updates$.subscribe(async (u: StatusUpdate) => {
        console.log('========================================================');
        console.log('[Employee Dashboard] << WebSocket status update received');
        console.log('[Employee Dashboard]   orderId   =', u.orderId);
        console.log('[Employee Dashboard]   newStatus =', u.newStatus);
        console.log('[Employee Dashboard]   reason    =', u.reason);
        console.log('========================================================');

        // Always record the update so it can be replayed after API refreshes
        this.allWsUpdates.push(u);

        // If orders haven't loaded yet, queue the update for later
        if (!this.ordersLoaded && this.orders.length === 0) {
          console.log('[Employee Dashboard] Orders not loaded yet, queuing update');
          this.pendingUpdates.push(u);
          return;
        }

        // Update table in-place — NO API call
        if (!this.applyStatusUpdate(u)) {
          // Order not in current list — queue it in case API data hasn't arrived yet
          if (!this.ordersLoaded) {
            console.log('[Employee Dashboard] Order not found yet, queuing for after API load');
            this.pendingUpdates.push(u);
          } else {
            console.log('[Employee Dashboard] Order not found in table, ignoring');
          }
        }
        this.cdr.detectChanges();
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
