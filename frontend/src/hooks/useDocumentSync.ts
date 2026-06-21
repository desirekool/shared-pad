import { useCallback, useRef, useEffect } from "react";
import { useWebSocket } from "./useWebSocket";
import type { IMessage } from "@stomp/stompjs";
import type { EditorOperation } from "../components/SyncEditor";
import { enqueueOperation, getQueue, clearQueue } from "../utils/offlineQueue";

interface UseDocumentSyncOptions {
  documentId: string;
  version: number;
  onRemoteOperation?: (op: EditorOperation) => void;
  onRejected?: () => void;
}

export function useDocumentSync({ documentId, version, onRemoteOperation, onRejected }: UseDocumentSyncOptions) {
  const versionRef = useRef(version);
  const applyOpRef = useRef<((op: EditorOperation) => void) | null>(null);
  const publishRef = useRef<((dest: string, body: unknown) => void) | null>(null);

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

          if (event.userId !== localStorage.getItem("username")) {
            applyOpRef.current?.(op);
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

  const { subscribe, publish, joinDocument, leaveDocument, unsubscribe, isConnected } = useWebSocket({
    onConnect: () => {
      subscribe(`/topic/document.${documentId}`, handleMessage);
      subscribe("/user/queue/errors", handleError);
      joinDocument(documentId);

      const queue = getQueue(documentId);
      if (queue.length > 0) {
        for (const msg of queue) {
          publishRef.current?.(msg.destination, JSON.parse(msg.body));
        }
        clearQueue(documentId);
        console.log(`Flushed ${queue.length} queued operations for doc ${documentId}`);
      }
    },
    onDisconnect: () => {
      unsubscribe("/user/queue/errors");
    },
  });

  publishRef.current = publish;

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
        enqueueOperation(documentId, destination, body);
        console.log("Offline: queued operation for doc", documentId);
      }
    },
    [documentId, publish, isConnected]
  );

  const setApplyOp = useCallback((fn: (op: EditorOperation) => void) => {
    applyOpRef.current = fn;
  }, []);

  return { sendOperation, setApplyOp, isConnected };
}
