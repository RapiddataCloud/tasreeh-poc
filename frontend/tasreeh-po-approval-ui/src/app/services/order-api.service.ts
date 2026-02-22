import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PurchaseOrder } from '../models/purchase-order.model';

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private base = `${environment.apiBaseUrl}/orders`;

  constructor(private http: HttpClient) { }

  /** Employee: get my submitted orders (filtered by JWT userId on backend) */
  getMyOrders(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(`${this.base}/my`);
  }

  /** Employee: create a new order */
  createOrder(payload: { item: string; amount: number; description: string }): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(this.base, payload);
  }

  /** Manager: get all orders */
  getAllOrders(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(this.base);
  }

  /** Manager: get pending orders */
  getPendingOrders(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(`${this.base}/pending`);
  }

  /** Manager: approve an order */
  approveOrder(id: string): Observable<any> {
    return this.http.put(`${this.base}/${id}/approve`, {});
  }

  /** Manager: reject an order */
  rejectOrder(id: string, reason: string): Observable<any> {
    return this.http.put(`${this.base}/${id}/reject`, { reason });
  }
}
