import type { LocalDocument } from "../types/localDocument";

const STORAGE_KEY = "syncdocs_local_docs";

export function loadLocalDocs(): LocalDocument[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveAll(docs: LocalDocument[]): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(docs));
}

export function createLocalDoc(title: string, content: string, filePath?: string): LocalDocument {
  const doc: LocalDocument = {
    id: "local_" + crypto.randomUUID(),
    title,
    content,
    filePath,
    isLocal: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  const all = loadLocalDocs();
  all.push(doc);
  saveAll(all);
  return doc;
}

export function getLocalDoc(id: string): LocalDocument | undefined {
  return loadLocalDocs().find((d) => d.id === id);
}

export function updateLocalDoc(id: string, content: string, title?: string): LocalDocument | null {
  const all = loadLocalDocs();
  const idx = all.findIndex((d) => d.id === id);
  if (idx === -1) return null;
  all[idx].content = content;
  if (title !== undefined) all[idx].title = title;
  all[idx].updatedAt = new Date().toISOString();
  saveAll(all);
  return all[idx];
}

export function deleteLocalDoc(id: string): void {
  saveAll(loadLocalDocs().filter((d) => d.id !== id));
}

export function removeLocalDoc(id: string): LocalDocument | null {
  const all = loadLocalDocs();
  const idx = all.findIndex((d) => d.id === id);
  if (idx === -1) return null;
  const doc = all[idx];
  all.splice(idx, 1);
  saveAll(all);
  return doc;
}
