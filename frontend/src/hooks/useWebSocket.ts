import { useEffect, useRef, useCallback } from "react";
import { Client, type IMessage, type Subscription } from "@stomp/stompjs";

const WS_URL = "ws://localhost:8080/ws";

interface UseWebSocketOptions {
  onConnect?: () => void;
  onDisconnect?: () => void;
}

export function useWebSocket(options?: UseWebSocketOptions) {
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, Subscription>>(new Map());

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) return;

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        options?.onConnect?.();
      },
      onDisconnect: () => {
        options?.onDisconnect?.();
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      subscriptionsRef.current.forEach((sub) => sub.unsubscribe());
      subscriptionsRef.current.clear();
      client.deactivate();
    };
  }, []);

  const subscribe = useCallback(
    (destination: string, callback: (msg: IMessage) => void) => {
      const client = clientRef.current;
      if (!client || !client.connected) {
        console.warn("WebSocket not connected, cannot subscribe to", destination);
        return;
      }

      const existing = subscriptionsRef.current.get(destination);
      if (existing) existing.unsubscribe();

      const subscription = client.subscribe(destination, callback);
      subscriptionsRef.current.set(destination, subscription);
    },
    []
  );

  const unsubscribe = useCallback((destination: string) => {
    const sub = subscriptionsRef.current.get(destination);
    if (sub) {
      sub.unsubscribe();
      subscriptionsRef.current.delete(destination);
    }
  }, []);

  const publish = useCallback(
    (destination: string, body: unknown) => {
      const client = clientRef.current;
      if (!client || !client.connected) {
        console.warn("WebSocket not connected, cannot publish to", destination);
        return;
      }
      client.publish({
        destination,
        body: JSON.stringify(body),
      });
    },
    []
  );

  const joinDocument = useCallback(
    (documentId: string) => {
      publish(`/app/documents.join.${documentId}`, { documentId });
    },
    [publish]
  );

  const leaveDocument = useCallback(
    (documentId: string) => {
      publish(`/app/documents.leave.${documentId}`, { documentId });
    },
    [publish]
  );

  return {
    subscribe,
    unsubscribe,
    publish,
    joinDocument,
    leaveDocument,
  };
}
