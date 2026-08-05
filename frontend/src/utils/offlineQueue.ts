const QUEUE_KEY_PREFIX = "syncdocs_offline_queue_";

interface QueuedMessage {
  destination: string;
  body: string;
  timestamp: number;
  documentId?: string;
  version?: number;
}

function getQueueKey(documentId: string): string {
  return QUEUE_KEY_PREFIX + documentId;
}

export function enqueueOperation(documentId: string, destination: string, body: unknown, version?: number): void {
  const key = getQueueKey(documentId);
  const queue = getQueue(documentId);
  queue.push({
    destination,
    body: JSON.stringify(body),
    timestamp: Date.now(),
    documentId,
    version,
  });
  localStorage.setItem(key, JSON.stringify(queue));
}

const MAX_AGE_MS = 30_000;

export function getQueue(documentId: string, maxAge = MAX_AGE_MS): QueuedMessage[] {
  const key = getQueueKey(documentId);
  const raw = localStorage.getItem(key);
  if (!raw) return [];
  const all: QueuedMessage[] = JSON.parse(raw);
  const now = Date.now();
  const fresh = all.filter((m) => now - m.timestamp < maxAge);
  if (fresh.length < all.length) {
    if (fresh.length > 0) {
      localStorage.setItem(key, JSON.stringify(fresh));
    } else {
      localStorage.removeItem(key);
    }
  }
  return fresh;
}

export function clearQueue(documentId: string): void {
  localStorage.removeItem(getQueueKey(documentId));
}

export function getQueueCount(documentId: string): number {
  return getQueue(documentId).length;
}

export function getAllQueues(): string[] {
  const keys: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key && key.startsWith(QUEUE_KEY_PREFIX)) {
      keys.push(key.replace(QUEUE_KEY_PREFIX, ""));
    }
  }
  return keys;
}
