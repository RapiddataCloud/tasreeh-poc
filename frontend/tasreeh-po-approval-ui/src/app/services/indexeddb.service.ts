import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PurchaseOrder } from '../models/purchase-order.model';

const DB_NAME = 'tasreeh-po-db';
const DB_VERSION = 1;
const STORE_NAME = 'orders';

@Injectable({ providedIn: 'root' })
export class IndexedDbService {
    private db: IDBDatabase | null = null;
    private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

    async open(): Promise<void> {
        if (!this.isBrowser || this.db) return;

        return new Promise((resolve, reject) => {
            const request = indexedDB.open(DB_NAME, DB_VERSION);

            request.onupgradeneeded = () => {
                const db = request.result;
                if (!db.objectStoreNames.contains(STORE_NAME)) {
                    db.createObjectStore(STORE_NAME, { keyPath: 'id' });
                }
            };

            request.onsuccess = () => {
                this.db = request.result;
                console.log('[IndexedDB] ✅ Database opened successfully');
                resolve();
            };

            request.onerror = () => {
                console.error('[IndexedDB] ❌ Failed to open database:', request.error);
                reject(request.error);
            };
        });
    }

    /** Store all orders (replaces existing data) */
    async storeOrders(orders: PurchaseOrder[]): Promise<void> {
        if (!this.db) return;

        return new Promise((resolve, reject) => {
            const tx = this.db!.transaction(STORE_NAME, 'readwrite');
            const store = tx.objectStore(STORE_NAME);

            // Clear existing data first
            store.clear();

            // Add all orders
            for (const order of orders) {
                store.put(order);
            }

            tx.oncomplete = () => {
                console.log(`[IndexedDB] Stored ${orders.length} orders`);
                resolve();
            };
            tx.onerror = () => reject(tx.error);
        });
    }

    /** Get all orders from IndexedDB */
    async getOrders(): Promise<PurchaseOrder[]> {
        if (!this.db) return [];

        return new Promise((resolve, reject) => {
            const tx = this.db!.transaction(STORE_NAME, 'readonly');
            const store = tx.objectStore(STORE_NAME);
            const request = store.getAll();

            request.onsuccess = () => {
                console.log(`[IndexedDB] Retrieved ${request.result.length} orders from cache`);
                resolve(request.result);
            };
            request.onerror = () => reject(request.error);
        });
    }

    /** Update a single order's status in IndexedDB */
    async updateOrderStatus(orderId: string, newStatus: string): Promise<void> {
        if (!this.db) return;

        return new Promise((resolve, reject) => {
            const tx = this.db!.transaction(STORE_NAME, 'readwrite');
            const store = tx.objectStore(STORE_NAME);
            const getReq = store.get(orderId);

            getReq.onsuccess = () => {
                const order = getReq.result;
                if (order) {
                    order.status = newStatus;
                    store.put(order);
                    console.log(`[IndexedDB] Updated order ${orderId} status → ${newStatus}`);
                }
                resolve();
            };
            getReq.onerror = () => reject(getReq.error);
        });
    }

    /** Add a single new order to IndexedDB */
    async addOrder(order: PurchaseOrder): Promise<void> {
        if (!this.db) return;

        return new Promise((resolve, reject) => {
            const tx = this.db!.transaction(STORE_NAME, 'readwrite');
            const store = tx.objectStore(STORE_NAME);
            store.put(order);

            tx.oncomplete = () => {
                console.log(`[IndexedDB] Added new order ${order.id}`);
                resolve();
            };
            tx.onerror = () => reject(tx.error);
        });
    }
}
