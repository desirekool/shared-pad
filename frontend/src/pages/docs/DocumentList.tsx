import { useEffect, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useDocuments, useDeleteDocument, useCreateDocument } from "../../api/hooks/useDocuments";
import { useAuth } from "../../context/AuthContext";
import { loadLocalDocs, createLocalDoc, deleteLocalDoc as removeLocalDoc } from "../../utils/localDocManager";
import { promoteLocalFile } from "../../api/documents";
import type { LocalDocument } from "../../types/localDocument";

function permissionBadge(level: string | undefined): string {
  switch (level) {
    case "OWNER": return "bg-blue-100 text-blue-800";
    case "EDITOR": return "bg-green-100 text-green-800";
    case "VIEWER": return "bg-slate-100 text-slate-600";
    default: return "bg-slate-100 text-slate-600";
  }
}

export default function DocumentList() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { data: docs, error, isLoading } = useDocuments();
  const deleteDoc = useDeleteDocument();
  const createDoc = useCreateDocument();
  const [localDocs, setLocalDocs] = useState<LocalDocument[]>([]);

  const handleLogout = () => {
    logout();
    navigate({ to: "/login" });
  };

  const refreshLocalDocs = () => setLocalDocs(loadLocalDocs());

  useEffect(() => {
    refreshLocalDocs();
  }, []);

  // File > Open as Local → create local doc
  useEffect(() => {
    const unsub = (window as any).electronAPI?.onFileOpened?.(async (file: { path: string; name: string; content: string }) => {
      createLocalDoc(file.name || "Untitled", file.content, file.path);
      refreshLocalDocs();
    });
    return () => unsub?.();
  }, []);

  // File > Open (Import to Server) → create server doc
  useEffect(() => {
    const unsub = (window as any).electronAPI?.onFileImported?.(async (file: { path: string; name: string; content: string }) => {
      try {
        const doc = await createDoc.mutateAsync({
          title: file.name || "Imported file",
          content: file.content,
        });
        navigate({ to: "/docs/$id", params: { id: doc.id.toString() } });
      } catch (e) {
        console.error("Failed to import file:", e);
      }
    });
    return () => unsub?.();
  }, [createDoc, navigate]);

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this document?")) return;
    deleteDoc.mutate(id);
  };

  const handleDeleteLocal = (id: string) => {
    if (!confirm("Delete this local file?")) return;
    removeLocalDoc(id);
    refreshLocalDocs();
  };

  const handlePromote = async (localDoc: LocalDocument) => {
    try {
      const doc = await promoteLocalFile({
        content: localDoc.content,
        originalFilename: localDoc.title,
        originalPath: localDoc.filePath,
        fileSize: new TextEncoder().encode(localDoc.content).length,
      });
      removeLocalDoc(localDoc.id);
      refreshLocalDocs();
      navigate({ to: "/docs/$id", params: { id: doc.id.toString() } });
    } catch (e) {
      console.error("Failed to promote local file:", e);
    }
  };

  return (
    <div className="flex flex-col h-screen">
      <div className="flex items-center gap-4 px-4 py-2 border-b bg-white shadow-sm">
        <span className="font-bold text-lg text-slate-900">SyncDocs</span>
        <div className="flex-1" />
        {user && (
          <>
            <span className="text-sm text-slate-500">{user.username}</span>
            <button
              onClick={handleLogout}
              className="bg-slate-100 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-slate-200 transition"
            >
              Logout
            </button>
          </>
        )}
      </div>
      <div className="p-6 flex-1 overflow-y-auto max-w-3xl mx-auto w-full">
        <h2 className="text-xl font-bold text-slate-900 mb-6">My Documents</h2>

        {error && (
          <div className="bg-red-50 text-red-600 px-4 py-2 rounded-lg text-sm mb-4">
            {(error as Error).message}
          </div>
        )}

        {/* Local Files */}
        {localDocs.length > 0 && (
          <div className="mb-8">
            <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Local Files</h3>
            <div className="space-y-1">
              {localDocs.map((doc) => (
                <div
                  key={doc.id}
                  className="flex items-center gap-3 px-4 py-3 hover:bg-slate-50 rounded-lg transition cursor-pointer"
                >
                  <span
                    className="flex-1 font-medium text-blue-600 hover:text-blue-800 text-sm"
                    onClick={() => navigate({ to: "/docs/$id", params: { id: doc.id } })}
                  >
                    {doc.title}
                  </span>
                  <span className="rounded-full bg-amber-100 text-amber-800 text-xs px-2 py-0.5 font-medium">
                    Local
                  </span>
                  <span className="text-xs text-slate-400">
                    {new Date(doc.updatedAt).toLocaleDateString()}
                  </span>
                  <button
                    onClick={() => handlePromote(doc)}
                    className="text-emerald-600 hover:text-emerald-800 text-xs font-medium transition"
                  >
                    Upload
                  </button>
                  <button
                    onClick={() => handleDeleteLocal(doc.id)}
                    className="text-red-500 hover:text-red-700 text-xs font-medium transition"
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Server Documents */}
        <div>
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Server Documents</h3>
          {isLoading ? (
            <p className="text-slate-400 text-sm">Loading...</p>
          ) : !docs || docs.length === 0 ? (
            <p className="text-slate-400 text-sm">No documents yet. Create one!</p>
          ) : (
            <div className="space-y-1">
              {docs.map((doc) => (
                <div
                  key={doc.id}
                  className="flex items-center gap-3 px-4 py-3 hover:bg-slate-50 rounded-lg transition cursor-pointer"
                >
                  <span
                    className="flex-1 font-medium text-blue-600 hover:text-blue-800 text-sm"
                    onClick={() => navigate({ to: "/docs/$id", params: { id: doc.id.toString() } })}
                  >
                    {doc.title}
                  </span>
                  <span className={`rounded-full text-xs px-2 py-0.5 font-medium ${permissionBadge(doc.permissionLevel)}`}>
                    {doc.permissionLevel || "OWNER"}
                  </span>
                  <span className="text-xs text-slate-400">
                    v{doc.version} &middot; {new Date(doc.updatedAt).toLocaleDateString()}
                  </span>
                  <button
                    onClick={() => doc.permissionLevel === "OWNER" && handleDelete(doc.id)}
                    className={`text-xs font-medium transition ${doc.permissionLevel === "OWNER" ? "text-red-500 hover:text-red-700" : "invisible"}`}
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="mt-6">
          <button
            onClick={() => navigate({ to: "/docs/$id", params: { id: "new" } })}
            className="bg-blue-600 text-white px-4 py-1.5 rounded-lg hover:bg-blue-700 transition text-sm font-medium"
          >
            + New
          </button>
        </div>
      </div>
    </div>
  );
}
