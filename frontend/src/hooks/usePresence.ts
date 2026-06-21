import { useState, useCallback, useEffect, useRef } from "react";
import { useWebSocket } from "./useWebSocket";
import type { IMessage } from "@stomp/stompjs";

interface CursorPosition {
  line: number;
  column: number;
}

interface SelectionRange {
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
}

export interface RemoteUser {
  userId: string;
  username: string;
  status: string;
  cursor?: CursorPosition;
  selection?: SelectionRange;
  typing: boolean;
}

interface UsePresenceOptions {
  documentId: string;
  onPresenceUpdate?: (users: RemoteUser[]) => void;
}

export function usePresence({ documentId, onPresenceUpdate }: UsePresenceOptions) {
  const [activeUsers, setActiveUsers] = useState<RemoteUser[]>([]);
  const usersRef = useRef<Map<string, RemoteUser>>(new Map());

  const handlePresence = useCallback((message: IMessage) => {
    try {
      const event = JSON.parse(message.body);
      const userId = event.userId;
      const payload = event.payload || {};

      switch (event.eventType) {
        case "USER_JOINED": {
          const user: RemoteUser = {
            userId,
            username: payload.username || userId,
            status: "ONLINE",
            typing: false,
          };
          usersRef.current.set(userId, user);
          break;
        }
        case "USER_LEFT": {
          usersRef.current.delete(userId);
          break;
        }
        case "CURSOR_UPDATE": {
          const existing = usersRef.current.get(userId);
          if (existing) {
            existing.cursor = payload.cursor;
            existing.selection = payload.selection;
            existing.lastActivity = Date.now();
          }
          break;
        }
        case "TYPING": {
          const existing = usersRef.current.get(userId);
          if (existing) {
            existing.typing = payload.typing || false;
          }
          break;
        }
      }

      const currentUser = localStorage.getItem("username");
      const updatedUsers = Array.from(usersRef.current.values())
        .filter((u) => u.userId !== currentUser);

      setActiveUsers(updatedUsers);
      onPresenceUpdate?.(updatedUsers);
    } catch (e) {
      console.error("Failed to parse presence event:", e);
    }
  }, [onPresenceUpdate]);

  const { subscribe, publish, joinDocument, leaveDocument, unsubscribe } = useWebSocket({
    onConnect: () => {
      subscribe(`/topic/document.${documentId}.presence`, handlePresence);
    },
    onDisconnect: () => {
      unsubscribe(`/topic/document.${documentId}.presence`);
    },
  });

  const sendCursor = useCallback(
    (cursor: CursorPosition, selection?: SelectionRange) => {
      publish(`/app/documents.${documentId}.cursor`, {
        cursor,
        selection,
      });
    },
    [documentId, publish]
  );

  const sendTyping = useCallback(
    (typing: boolean) => {
      publish(`/app/documents.${documentId}.typing`, { typing });
    },
    [documentId, publish]
  );

  return { activeUsers, sendCursor, sendTyping, joinDocument, leaveDocument };
}
