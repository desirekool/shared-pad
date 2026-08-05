import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import * as api from "../documents";

export const documentKeys = {
  all: ["documents"] as const,
  lists: () => [...documentKeys.all, "list"] as const,
  details: () => [...documentKeys.all, "detail"] as const,
  detail: (id: number) => [...documentKeys.details(), id] as const,
  versions: (id: number) => [...documentKeys.detail(id), "versions"] as const,
  permissions: (id: number) => [...documentKeys.detail(id), "permissions"] as const,
};

export function useDocuments() {
  return useQuery({
    queryKey: documentKeys.lists(),
    queryFn: api.listDocuments,
  });
}

export function useDocument(id: number) {
  return useQuery({
    queryKey: documentKeys.detail(id),
    queryFn: () => api.getDocument(id),
    enabled: !!id,
  });
}

export function useCreateDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { title: string; content: string; mimeType?: string }) =>
      api.createDocument(vars.title, vars.content, vars.mimeType),
    onSuccess: () => qc.invalidateQueries({ queryKey: documentKeys.lists() }),
  });
}

export function useUpdateDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { id: number; data: { title?: string; content?: string } }) =>
      api.updateDocument(vars.id, vars.data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: documentKeys.detail(id) });
      qc.invalidateQueries({ queryKey: documentKeys.lists() });
    },
  });
}

export function useDeleteDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.deleteDocument,
    onSuccess: () => qc.invalidateQueries({ queryKey: documentKeys.lists() }),
  });
}

export function useVersions(documentId: number) {
  return useQuery({
    queryKey: documentKeys.versions(documentId),
    queryFn: () => api.getVersions(documentId),
    enabled: !!documentId,
  });
}

export function useVersionContent(documentId: number, version: number) {
  return useQuery({
    queryKey: [...documentKeys.versions(documentId), version],
    queryFn: () => api.getVersionContent(documentId, version),
    enabled: !!documentId && version > 0,
  });
}

export function useRestoreVersion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { id: number; version: number }) => api.restoreVersion(vars.id, vars.version),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: documentKeys.detail(id) });
      qc.invalidateQueries({ queryKey: documentKeys.versions(id) });
    },
  });
}

export function usePermissions(documentId: number) {
  return useQuery({
    queryKey: documentKeys.permissions(documentId),
    queryFn: () => api.listPermissions(documentId),
    enabled: !!documentId,
  });
}

export function useShareDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { documentId: number; username: string; permissionLevel: string }) =>
      api.shareDocument(vars.documentId, vars.username, vars.permissionLevel),
    onSuccess: (_, { documentId }) => {
      qc.invalidateQueries({ queryKey: documentKeys.permissions(documentId) });
    },
  });
}
