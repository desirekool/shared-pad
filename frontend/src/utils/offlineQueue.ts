const QUEUE_KEY_PREFIX = "syncdocs_offline_queue_";

interface QueuedMessage {
  destination: string;
  body: string;
  timestamp: number;
  documentId?: string;
}

function getQueueKey(documentId: string): string {
  return QUEUE_KEY_PREFIX + documentId;
}

export function enqueueOperation(documentId: string, destination: string, body: unknown): void {
  const key = getQueueKey(documentId);
  const queue = getQueue(documentId);
  queue.push({
    destination,
    body: JSON.stringify(body),
    timestamp: Date.now(),
    documentId,
  });
  localStorage.setItem(key, JSON.stringify(queue));
}

export function getQueue(documentId: string): QueuedMessage[] {
  const key = getQueueKey(documentId);
  const raw = localStorage.getItem(key);
  return raw ? JSON.parse(raw) : [];
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
