import { Injectable, NgZone, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Subject } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../environments/environment';

export type StatusUpdate = {
  orderId: string;
  newStatus: string;
  reason?: string;
  timestamp?: string;
};

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private ws?: WebSocket;
  private updatesSubject = new Subject<StatusUpdate>();
  updates$ = this.updatesSubject.asObservable();
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private reconnectTimer: any;

  /** Track the timestamp of the last received event for state recovery on reconnect */
  private lastEventTimestamp: string | null = null;

  constructor(private keycloak: KeycloakService, private zone: NgZone) { }

  async connect() {
    if (!this.isBrowser) return;
    if (this.ws?.readyState === WebSocket.OPEN || this.ws?.readyState === WebSocket.CONNECTING) return;

    const token = await this.keycloak.getToken();
    let wsUrl = `${environment.wsUrl}?token=${encodeURIComponent(token)}`;

    // State recovery: include lastEventTime on reconnect so server replays missed events
    if (this.lastEventTimestamp) {
      wsUrl += `&lastEventTime=${encodeURIComponent(this.lastEventTimestamp)}`;
      console.log('[ANGULAR-WS] Reconnecting with state recovery since:', this.lastEventTimestamp);
    }

    console.log('========================================================');
    console.log('[ANGULAR-WS] Connecting to WebSocket...');
    console.log('[ANGULAR-WS]   url =', environment.wsUrl);
    console.log('========================================================');

    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('========================================================');
      console.log('[ANGULAR-WS]  WebSocket CONNECTED successfully');
      console.log('========================================================');
    };

    this.ws.onmessage = (event) => {
      this.zone.run(() => {
        try {
          const update = JSON.parse(event.data) as StatusUpdate;
          console.log('========================================================');
          console.log('[ANGULAR-WS] << RECEIVED WebSocket message');
          console.log('[ANGULAR-WS]   orderId   =', update.orderId);
          console.log('[ANGULAR-WS]   newStatus =', update.newStatus);
          console.log('[ANGULAR-WS]   reason    =', update.reason);
          console.log('========================================================');

          // Track last event timestamp for reconnection recovery
          if (update.timestamp) {
            this.lastEventTimestamp = update.timestamp;
          }

          this.updatesSubject.next(update);
        } catch (e) {
          console.error('[ANGULAR-WS] Failed to parse message:', event.data);
        }
      });
    };

    this.ws.onerror = (err) => {
      console.error('========================================================');
      console.error('[ANGULAR-WS]  WebSocket ERROR:', err);
      console.error('========================================================');
    };

    this.ws.onclose = (e) => {
      console.log('========================================================');
      console.log('[ANGULAR-WS] WebSocket DISCONNECTED, code:', e.code, 'reason:', e.reason);
      console.log('[ANGULAR-WS] Will reconnect in 5 seconds with state recovery...');
      console.log('========================================================');
      this.ws = undefined;
      this.reconnectTimer = setTimeout(() => this.connect(), 5000);
    };
  }

  /** Request recovery of missed events while connected */
  requestRecovery(sinceTimestamp: string) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type: 'recover', since: sinceTimestamp }));
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = undefined;
    }
    this.ws?.close();
    this.ws = undefined;
  }
}
