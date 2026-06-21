import { useEffect, useRef, useCallback, useState } from "react";
import { Client, type IMessage, type Subscription } from "@stomp/stompjs";

const WS_URL = "ws://localhost:8080/ws";

let sharedClient: Client | null = null;
let sharedRefCount = 0;
const connectCallbacks = new Set<() => void>();
const disconnectCallbacks = new Set<() => void>();

function getClient(): Client {
  if (!sharedClient) {
    const token = localStorage.getItem("token");
    sharedClient = new Client({
      brokerURL: WS_URL,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connectCallbacks.forEach((cb) => cb());
      },
      onDisconnect: () => {
        disconnectCallbacks.forEach((cb) => cb());
      },
    });
    sharedClient.activate();
  }
  return sharedClient;
}

interface UseWebSocketOptions {
  onConnect?: () => void;
  onDisconnect?: () => void;
}

export function useWebSocket(options?: UseWebSocketOptions) {
  const subscriptionsRef = useRef<Map<string, Subscription>>(new Map());
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    sharedRefCount++;

    const handleConnect = () => {
      setIsConnected(true);
      options?.onConnect?.();
    };

    const handleDisconnect = () => {
      setIsConnected(false);
      options?.onDisconnect?.();
    };

    connectCallbacks.add(handleConnect);
    disconnectCallbacks.add(handleDisconnect);

    const client = getClient();
    if (client.connected) {
      setIsConnected(true);
      options?.onConnect?.();
    }

    return () => {
      sharedRefCount--;
      connectCallbacks.delete(handleConnect);
      disconnectCallbacks.delete(handleDisconnect);

      subscriptionsRef.current.forEach((sub) => sub.unsubscribe());
      subscriptionsRef.current.clear();

      if (sharedRefCount <= 0 && sharedClient) {
        sharedClient.deactivate();
        sharedClient = null;
      }
    };
  }, []);

  const subscribe = useCallback(
    (destination: string, callback: (msg: IMessage) => void) => {
      const client = sharedClient;
      if (!client || !client.connected) {
        console.warn("WebSocket not connected, cannot subscribe to", destination);
        return;
      }

      const existing = subscriptionsRef.current.get(destination);
      if (existing) existing.unsubscribe();

      const subscription = client.subscribe(destination, (msg) => {
        try {
          callback(msg);
        } catch (e) {
          console.error("Error in message handler for", destination, e);
        }
      });
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
      const client = sharedClient;
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
      const username = localStorage.getItem("username") || "anonymous";
      publish(`/app/documents.join.${documentId}`, { documentId, username });
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
    isConnected,
  };
}
