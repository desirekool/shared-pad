import { useCallback, useRef, useEffect } from "react";
import { useWebSocket } from "./useWebSocket";
import type { IMessage } from "@stomp/stompjs";
import type { EditorOperation } from "../components/SyncEditor";

interface UseDocumentSyncOptions {
  documentId: string;
  version: number;
  onRemoteOperation?: (op: EditorOperation) => void;
}

export function useDocumentSync({ documentId, version, onRemoteOperation }: UseDocumentSyncOptions) {
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

  const { subscribe, unsubscribe, publish, joinDocument, leaveDocument } = useWebSocket({
    onConnect: () => {
      subscribe(`/topic/document.${documentId}`, handleMessage);
      joinDocument(documentId);
    },
    onDisconnect: () => {
      leaveDocument(documentId);
    },
  });

  const sendOperation = useCallback(
    (op: EditorOperation) => {
      publish(`/app/document.${documentId}.edit`, {
        type: op.type,
        position: op.position,
        text: op.text,
        length: op.length,
        version: op.version,
      });
    },
    [documentId, publish]
  );

  const setApplyOp = useCallback((fn: (op: EditorOperation) => void) => {
    applyOpRef.current = fn;
  }, []);

  return { sendOperation, setApplyOp };
}
