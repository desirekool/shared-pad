const API_BASE = "http://localhost:8080/api";

function getHeaders(): HeadersInit {
  const token = localStorage.getItem("token");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export interface DocumentResponse {
  id: number;
  title: string;
  owner: string;
  mimeType: string;
  size: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  content: string | null;
  originalFilename: string | null;
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Request failed" }));
    throw new Error(error.message);
  }
  return res.json();
}

export async function listDocuments(): Promise<DocumentResponse[]> {
  const res = await fetch(`${API_BASE}/documents`, { headers: getHeaders() });
  return handleResponse<DocumentResponse[]>(res);
}

export async function getDocument(id: number): Promise<DocumentResponse> {
  const res = await fetch(`${API_BASE}/documents/${id}`, { headers: getHeaders() });
  return handleResponse<DocumentResponse>(res);
}

export async function createDocument(title: string, content: string, mimeType?: string): Promise<DocumentResponse> {
  const res = await fetch(`${API_BASE}/documents`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ title, content, mimeType }),
  });
  return handleResponse<DocumentResponse>(res);
}

export async function updateDocument(id: number, data: { title?: string; content?: string }): Promise<DocumentResponse> {
  const res = await fetch(`${API_BASE}/documents/${id}`, {
    method: "PUT",
    headers: getHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse<DocumentResponse>(res);
}

export async function deleteDocument(id: number): Promise<void> {
  const res = await fetch(`${API_BASE}/documents/${id}`, {
    method: "DELETE",
    headers: getHeaders(),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Delete failed" }));
    throw new Error(error.message);
  }
}

export async function promoteLocalFile(data: {
  content: string;
  originalFilename: string;
  originalPath?: string;
  originalChecksum?: string;
  mimeType?: string;
  fileSize: number;
  originalLastModified?: string;
}): Promise<DocumentResponse> {
  const res = await fetch(`${API_BASE}/documents/promote`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse<DocumentResponse>(res);
}

export async function uploadDocument(file: File): Promise<DocumentResponse> {
  const token = localStorage.getItem("token");
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch(`${API_BASE}/documents/upload`, {
    method: "POST",
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: formData,
  });
  return handleResponse<DocumentResponse>(res);
}

export interface DocumentVersion {
  id: number;
  documentId: number;
  versionNumber: number;
  createdBy: string;
  createdAt: string;
  message: string;
}

export async function getVersions(id: number): Promise<DocumentVersion[]> {
  const res = await fetch(`${API_BASE}/documents/${id}/versions`, { headers: getHeaders() });
  return handleResponse<DocumentVersion[]>(res);
}

export async function getVersionContent(id: number, version: number): Promise<{ content: string; version: number; title: string }> {
  const res = await fetch(`${API_BASE}/documents/${id}/versions/${version}`, { headers: getHeaders() });
  return handleResponse(res);
}

export async function restoreVersion(id: number, version: number): Promise<DocumentResponse> {
  const res = await fetch(`${API_BASE}/documents/${id}/versions/${version}/restore`, {
    method: "POST",
    headers: getHeaders(),
  });
  return handleResponse<DocumentResponse>(res);
}

export async function downloadDocument(id: number): Promise<Blob> {
  const token = localStorage.getItem("token");
  const res = await fetch(`${API_BASE}/documents/${id}/download`, {
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  });
  if (!res.ok) throw new Error("Download failed");
  return res.blob();
}
