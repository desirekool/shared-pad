import { useCallback, useRef, useEffect } from "react";
import { useWebSocket, sharedClient } from "./useWebSocket";
import type { IMessage } from "@stomp/stompjs";
import type { EditorOperation } from "../components/SyncEditor";
import type { StoredEvent } from "../api/documents";
import { getDocumentEvents } from "../api/documents";
import { enqueueOperation, getQueue, clearQueue } from "../utils/offlineQueue";

const LAST_EVENT_KEY_PREFIX = "syncdocs_last_event_";

function getLastEventKey(docId: string) { return LAST_EVENT_KEY_PREFIX + docId; }
function getLastEventId(docId: string): number | null {
  const raw = localStorage.getItem(getLastEventKey(docId));
  return raw ? Number(raw) : null;
}
function setLastEventId(docId: string, id: number) {
  localStorage.setItem(getLastEventKey(docId), String(id));
}

interface UseDocumentSyncOptions {
  documentId: string;
  version: number;
  username?: string | null;
  onRemoteOperation?: (op: EditorOperation) => void;
  onRejected?: () => void;
  onPermissionRevoked?: (payload: { documentId: number }) => void;
}

export function useDocumentSync({ documentId, version, username, onRemoteOperation, onRejected, onPermissionRevoked }: UseDocumentSyncOptions) {
  const versionRef = useRef(version);
  const applyOpRef = useRef<((op: EditorOperation) => void) | null>(null);

  useEffect(() => {
    versionRef.current = version;
  }, [version]);

  const handleMessage = useCallback(
    (message: IMessage) => {
      try {
        const event = JSON.parse(message.body);
        if (event.eventType === "EDIT" && event.payload) {
          const op: EditorOperation = {
            type: event.payload.type as EditorOperation["type"],
            position: event.payload.position,
            text: event.payload.text,
            length: event.payload.length,
            version: event.payload.version,
          };

          if (event.userId !== username) {
            onRemoteOperation?.(op);
          }
        }
      } catch (e) {
        console.error("Failed to parse remote operation:", e);
      }
    },
    [onRemoteOperation]
  );

  const handleError = useCallback(
    (message: IMessage) => {
      try {
        const result = JSON.parse(message.body);
        if (result.accepted === false) {
          console.warn("Edit rejected by server:", result.reason);
          onRejected?.();
        }
      } catch (e) {
        console.error("Failed to parse error message:", e);
      }
    },
    [onRejected]
  );

  const handlePermission = useCallback(
    (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        if (payload.type === "REVOKED" && payload.documentId === Number(documentId)) {
          onPermissionRevoked?.(payload);
        }
      } catch (e) {
        console.error("Failed to parse permission message:", e);
      }
    },
    [documentId, onPermissionRevoked]
  );

  async function fetchAndReplayEvents() {
    const afterId = getLastEventId(documentId);
    try {
      const events: StoredEvent[] = await getDocumentEvents(Number(documentId), afterId ?? undefined);
      let maxId = afterId ?? 0;
      for (const ev of events) {
        if (ev.id > maxId) maxId = ev.id;
        if (ev.eventType !== "EDIT" || !ev.payload || ev.userId === username) continue;
        try {
          const payload = JSON.parse(ev.payload);
          const op: EditorOperation = {
            type: payload.type as EditorOperation["type"],
            position: payload.position,
            text: payload.text,
            length: payload.length,
            version: payload.version,
          };
          applyOpRef.current?.(op);
        } catch { /* skip unparseable */ }
      }
      if (maxId > (afterId ?? 0)) {
        setLastEventId(documentId, maxId);
      }
    } catch (e) {
      console.warn("Failed to fetch missed events for doc", documentId, e);
    }
  }

  function flushOfflineQueue() {
    const queue = getQueue(documentId);
    const currentVersion = versionRef.current;
    if (queue.length === 0) return;
    const fresh = queue.filter((m) => !m.version || m.version >= currentVersion);
    for (const msg of fresh) {
      const body = JSON.parse(msg.body);
      if (sharedClient?.connected) {
        sharedClient.publish({
          destination: msg.destination,
          body: JSON.stringify(body),
        });
      }
    }
    clearQueue(documentId);
    console.log(`Flushed ${fresh.length}/${queue.length} queued operations for doc ${documentId}`);
  }

  const { subscribe, publish, joinDocument, unsubscribe, isConnected } = useWebSocket({
    onConnect: async () => {
      subscribe(`/topic/document.${documentId}`, handleMessage);
      subscribe("/user/queue/errors", handleError);
      subscribe("/user/queue/permissions", handlePermission);

      await fetchAndReplayEvents();

      joinDocument(documentId);

      flushOfflineQueue();
    },
    onDisconnect: () => {
      unsubscribe("/user/queue/errors");
      unsubscribe("/user/queue/permissions");
    },
  });

  const sendOperation = useCallback(
    (op: EditorOperation) => {
      const destination = `/app/document.${documentId}.edit`;
      const body = {
        type: op.type,
        position: op.position,
        text: op.text,
        length: op.length,
        version: op.version,
      };

      if (isConnected) {
        publish(destination, body);
      } else {
        enqueueOperation(documentId, destination, body, op.version);
      }
    },
    [documentId, publish, isConnected]
  );

  const setApplyOp = useCallback((fn: (op: EditorOperation) => void) => {
    applyOpRef.current = fn;
  }, []);

  return { sendOperation, setApplyOp, isConnected };
}
