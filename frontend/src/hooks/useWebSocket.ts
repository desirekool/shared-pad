import { useEffect, useRef, useCallback, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";

const WS_URL = "ws://localhost:8080/ws";

export let sharedClient: Client | null = null;
let sharedRefCount = 0;
let deactivateTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectFailCount = 0;
const MAX_RECONNECT_FAILURES = 5;
const connectCallbacks = new Set<() => void>();
const disconnectCallbacks = new Set<() => void>();
const errorCallbacks = new Set<(msg: string) => void>();

function buildClient(): Client {
  const token = localStorage.getItem("token");
  const client = new Client({
    brokerURL: WS_URL,
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      reconnectFailCount = 0;
      connectCallbacks.forEach((cb) => cb());
    },
    onDisconnect: () => {
      disconnectCallbacks.forEach((cb) => cb());
    },
    onStompError: (frame) => {
      console.error("STOMP error:", frame.headers["message"]);
      errorCallbacks.forEach((cb) => cb("STOMP: " + frame.headers["message"]));
    },
    onWebSocketError: () => {
      reconnectFailCount++;
      console.error("WebSocket error (attempt", reconnectFailCount + ")");
      if (reconnectFailCount >= MAX_RECONNECT_FAILURES) {
        console.warn("Too many reconnect failures, recreating client");
        const old = sharedClient;
        sharedClient = null;
        old?.deactivate().catch(() => {});
        ensureClient();
      }
      errorCallbacks.forEach((cb) => cb("WebSocket connection error"));
    },
  });
  return client;
}

function ensureClient(): Client {
  if (!sharedClient) {
    sharedClient = buildClient();
    sharedClient.activate();
  }
  return sharedClient;
}

function scheduleDestroy(delayMs = 100) {
  if (deactivateTimer) return;
  deactivateTimer = setTimeout(() => {
    deactivateTimer = null;
    const c = sharedClient;
    if (c) {
      sharedClient = null;
      c.deactivate().catch((err) => console.error("deactivate error:", err));
    }
  }, delayMs);
}

function cancelDestroy() {
  if (deactivateTimer) {
    clearTimeout(deactivateTimer);
    deactivateTimer = null;
  }
}

interface UseWebSocketOptions {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (msg: string) => void;
}

export function useWebSocket(options?: UseWebSocketOptions) {
  const subscriptionsRef = useRef<Map<string, { unsubscribe(): void }>>(new Map());
  const [isConnected, setIsConnected] = useState(() => sharedClient?.connected ?? false);
  const optionsRef = useRef(options);
  optionsRef.current = options;

  useEffect(() => {
    sharedRefCount++;
    cancelDestroy();

    const handleConnect = () => {
      console.log("WebSocket CONNECTED, isConnected -> true");
      setIsConnected(true);
      optionsRef.current?.onConnect?.();
    };

    const handleDisconnect = () => {
      setIsConnected(false);
      optionsRef.current?.onDisconnect?.();
    };

    const handleError = (msg: string) => {
      optionsRef.current?.onError?.(msg);
    };

    connectCallbacks.add(handleConnect);
    disconnectCallbacks.add(handleDisconnect);
    errorCallbacks.add(handleError);

    const client = ensureClient();
    if (client.connected) {
      handleConnect();
    }

    return () => {
      sharedRefCount--;
      connectCallbacks.delete(handleConnect);
      disconnectCallbacks.delete(handleDisconnect);
      errorCallbacks.delete(handleError);

      subscriptionsRef.current.forEach((sub) => sub.unsubscribe());
      subscriptionsRef.current.clear();

      if (sharedRefCount <= 0) {
        scheduleDestroy();
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
      console.log("STOMP publish:", destination, body);
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
