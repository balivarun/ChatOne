import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL =
  process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws';

let client: Client | null = null;
const subscriptions = new Map<string, StompSubscription>();

export function connectWebSocket(token: string, onConnected?: () => void) {
  if (client?.active) return;
  client = new Client({
    webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    onConnect: () => {
      onConnected?.();
    },
    onDisconnect: () => {
      subscriptions.clear();
    },
    onStompError: (frame) => console.error('STOMP error', frame),
  });
  client.activate();
}

export function disconnectWebSocket() {
  subscriptions.forEach((s) => s.unsubscribe());
  subscriptions.clear();
  client?.deactivate();
  client = null;
}

export function subscribe(
  destination: string,
  callback: (msg: IMessage) => void
): string {
  if (!client?.active) return destination;
  if (subscriptions.has(destination)) return destination;
  const sub = client.subscribe(destination, callback);
  subscriptions.set(destination, sub);
  return destination;
}

export function unsubscribe(destination: string) {
  subscriptions.get(destination)?.unsubscribe();
  subscriptions.delete(destination);
}

export function sendMessage(destination: string, body: unknown) {
  client?.publish({ destination, body: JSON.stringify(body) });
}

export function isConnected() {
  return !!client?.active;
}
