import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useNavigate } from "@tanstack/react-router";
import { useDocument, useUpdateDocument, useCreateDocument } from "../../api/hooks/useDocuments";
import SyncEditor, { type EditorOperation } from "../../components/SyncEditor";
import { useDocumentSync } from "../../hooks/useDocumentSync";
import { useAuth } from "../../context/AuthContext";
import { usePresence } from "../../hooks/usePresence";
import PresenceBar from "../../components/PresenceBar";
import VersionPanel from "../../components/VersionPanel";
import ShareDialog from "../../components/ShareDialog";
import { getQueueCount } from "../../utils/offlineQueue";
import { getLocalDoc, updateLocalDoc, removeLocalDoc } from "../../utils/localDocManager";
import { promoteLocalFile } from "../../api/documents";

export default function DocumentEditor() {
  const { user, logout } = useAuth();
  const { id } = useParams({ from: "/docs/$id" });
  const navigate = useNavigate();
  const numId = id ? Number(id) : 0;
  const isNew = id === "new" || !id;
  const isLocal = !isNew && id?.startsWith("local_");
  const { data: doc, error, refetch } = useDocument(isLocal || isNew ? 0 : numId);
  const [content, setContent] = useState("");
  const [title, setTitle] = useState("");
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [wsError, setWsError] = useState("");
  const [showHistory, setShowHistory] = useState(false);
  const [showShare, setShowShare] = useState(false);
  const [showToolbar, setShowToolbar] = useState(true);
  const [localDocLoaded, setLocalDocLoaded] = useState(false);
  const applyOpRef = useRef<((op: EditorOperation) => void) | null>(null);
  const contentRef = useRef(content);
  const titleRef = useRef(title);
  const saveCtxRef = useRef({ doc, isReadOnly: false, updateDoc: null as any, createDoc: null as any });
  const hasEditedRef = useRef(false);

  contentRef.current = content;
  titleRef.current = title;

  const isOwner = isLocal ? false : doc?.permissionLevel === "OWNER";
  const isReadOnly = isLocal ? false : doc?.permissionLevel === "VIEWER";

  const { activeUsers, joinDocument, leaveDocument } = usePresence({
    documentId: id || "new",
  });

  // Set window title (OS title bar for Electron)
  useEffect(() => {
    if (isNew) {
      document.title = "SyncDocs — New Document";
    } else if (title) {
      document.title = `SyncDocs — ${title}`;
    } else {
      document.title = "SyncDocs";
    }
    return () => { document.title = "SyncDocs"; };
  }, [title, isNew]);

  // Electron IPC: update native window title
  useEffect(() => {
    const titleStr = isNew
      ? "SyncDocs — New Document"
      : title
        ? `SyncDocs — ${title}`
        : "SyncDocs";
    (window as any).electronAPI?.setTitle?.(titleStr);
  }, [title, isNew]);

  // Load local doc content
  useEffect(() => {
    if (isLocal && id) {
      const local = getLocalDoc(id);
      if (local) {
        setTitle(local.title);
        setContent(local.content);
        setLocalDocLoaded(true);
      }
    }
  }, [isLocal, id]);

  // Load server doc content
  useEffect(() => {
    if (doc && !isLocal) {
      setTitle(doc.title);
      setContent(doc.content || "");
    }
  }, [doc, isLocal]);

  useEffect(() => {
    if (!id || isNew || isLocal) return;
    joinDocument(id);
    return () => leaveDocument(id);
  }, [id, isNew, isLocal, joinDocument, leaveDocument]);

  const handleRejected = useCallback(() => {
    setWsError("Edit rejected due to version conflict. Re-fetching...");
    refetch();
  }, [refetch]);

  const handleRemoteOp = useCallback((op: EditorOperation) => {
    applyOpRef.current?.(op);
  }, []);

  const handlePermissionRevoked = useCallback(() => {
    navigate({ to: "/docs" });
  }, [navigate]);

  // Redirect on access-denied or not-found errors
  useEffect(() => {
    const errMsg = (error as Error)?.message || "";
    if (errMsg.includes("Access denied") || errMsg.includes("Document not found") || errMsg.includes("Permission denied")) {
      const t = setTimeout(() => navigate({ to: "/docs" }), 1500);
      return () => clearTimeout(t);
    }
  }, [error, navigate]);

  const { sendOperation, setApplyOp, isConnected } = useDocumentSync({
    documentId: id || "new",
    version: doc?.version || 0,
    username: user?.username,
    onRemoteOperation: handleRemoteOp,
    onRejected: handleRejected,
    onPermissionRevoked: handlePermissionRevoked,
  });

  const [queuedCount, setQueuedCount] = useState(0);

  useEffect(() => {
    if (!isConnected) {
      const interval = setInterval(() => {
        setQueuedCount(getQueueCount(id || "new"));
      }, 2000);
      return () => clearInterval(interval);
    }
    setQueuedCount(0);
  }, [isConnected, id]);

  useEffect(() => {
    setApplyOp((op: EditorOperation) => {
      applyOpRef.current?.(op);
    });
  }, [setApplyOp]);

  const handleOperation = useCallback(
    (op: EditorOperation) => {
      if (isReadOnly) return;
      if (!hasEditedRef.current) {
        hasEditedRef.current = true;
        setShowToolbar(false);
      }
      sendOperation(op);
      // Auto-save local doc content
      if (isLocal && id) {
        updateLocalDoc(id, contentRef.current, titleRef.current);
      }
    },
    [sendOperation, isReadOnly, isLocal, id]
  );

  const updateDoc = useUpdateDocument();
  const createDoc = useCreateDocument();

  const handleSave = async () => {
    if (isNew) {
      setSaving(true);
      try {
        const newDoc = await createDoc.mutateAsync({ title: title || "Untitled", content });
        navigate({ to: "/docs/$id", params: { id: newDoc.id.toString() }, replace: true });
      } catch (e) {
        setWsError(e instanceof Error ? e.message : "Creation failed");
      } finally {
        setSaving(false);
      }
      return;
    }
    if (isLocal) {
      if ((window as any).electronAPI) {
        const local = getLocalDoc(id!);
        if (local?.filePath) {
          await (window as any).electronAPI.saveFile(local.filePath, content);
        } else {
          await (window as any).electronAPI.saveAs(content, title || "document.txt");
        }
      }
      if (id) updateLocalDoc(id, content, title);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
      return;
    }
    if (!doc || isReadOnly) return;
    setSaving(true);
    try {
      await updateDoc.mutateAsync({ id: doc.id, data: { title, content } });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (e) {
      setWsError(e instanceof Error ? e.message : "Save failed");
    } finally {
      setSaving(false);
    }
  };

  const handlePromote = async () => {
    if (!isLocal || !id) return;
    setSaving(true);
    try {
      const local = getLocalDoc(id);
      if (!local) return;
      const serverDoc = await promoteLocalFile({
        content: local.content,
        originalFilename: local.title,
        originalPath: local.filePath,
        fileSize: new TextEncoder().encode(local.content).length,
      });
      removeLocalDoc(id);
      navigate({ to: "/docs/$id", params: { id: serverDoc.id.toString() }, replace: true });
    } catch (e) {
      setWsError(e instanceof Error ? e.message : "Upload failed");
    } finally {
      setSaving(false);
    }
  };

  // Electron menu handlers (uses refs to avoid stale closures)
  saveCtxRef.current = { doc, isReadOnly, updateDoc, createDoc };

  useEffect(() => {
    const unsubSave = (window as any).electronAPI?.onSaveRequested?.(async () => {
      if (isLocal) {
        const local = getLocalDoc(id!);
        if (local?.filePath) {
          await (window as any).electronAPI.saveFile(local.filePath, contentRef.current);
        } else {
          await (window as any).electronAPI.saveAs(contentRef.current, titleRef.current || "document.txt");
        }
        return;
      }
      const { doc, isReadOnly, updateDoc } = saveCtxRef.current;
      if (isReadOnly) return;
      setSaving(true);
      try {
        if (doc) {
          await updateDoc.mutateAsync({ id: doc.id, data: { title: titleRef.current, content: contentRef.current } });
        }
      } catch (e) {
        setWsError(e instanceof Error ? e.message : "Save failed");
      } finally {
        setSaving(false);
      }
    });
    const unsubSaveAs = (window as any).electronAPI?.onSaveAsRequested?.(() => {
      window.electronAPI?.saveAs(contentRef.current, titleRef.current || "document.txt");
    });
    const unsubOpen = (window as any).electronAPI?.onFileOpened?.(
      (file: { path: string; name: string; content: string }) => {
        setContent(file.content);
        setTitle(file.name);
      }
    );
    return () => {
      unsubOpen?.();
      unsubSave?.();
      unsubSaveAs?.();
    };
  }, [isLocal, id]);

  const handleRestore = useCallback((restoredContent: string) => {
    setContent(restoredContent);
    setShowHistory(false);
  }, []);

  const showContent = isLocal ? localDocLoaded : (!!doc || isNew);

  const toggleToolbar = useCallback(() => {
    setShowToolbar((prev) => !prev);
  }, []);

  return (
    <div className="flex flex-col h-screen">
      {/* Always-visible header row */}
      <div className="flex items-center gap-2 px-4 py-2 border-b bg-white shadow-sm">
        <button
          onClick={() => navigate({ to: "/docs" })}
          className="text-lg px-2 py-1 rounded hover:bg-slate-100 transition leading-none"
          title="Back to documents"
        >
          ←
        </button>
        <div className="flex-1" />
        <button
          onClick={toggleToolbar}
          className="text-sm px-2 py-1 rounded hover:bg-slate-100 transition text-slate-500"
          title={showToolbar ? "Hide toolbar" : "Show toolbar"}
        >
          {showToolbar ? "▲" : "▼"}
        </button>
      </div>

      {/* Collapsible toolbar */}
      <div
        className={`grid transition-all duration-300 ease-in-out ${
          showToolbar ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
        }`}
      >
        <div className="overflow-hidden border-b bg-white">
          <div className="flex flex-wrap items-center gap-2 px-4 py-2">
          <span className="font-bold text-base text-slate-900 mr-1">SyncDocs</span>
          <input
            value={title}
            onChange={(e) => {
              setTitle(e.target.value);
              if (isLocal && id) updateLocalDoc(id, content, e.target.value);
            }}
            maxLength={255}
            className="text-lg font-medium px-2 py-1 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 min-w-[120px] flex-1 outline-none"
            readOnly={isReadOnly}
          />
          {isLocal && (
            <>
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-3 py-1.5 rounded-lg text-sm font-medium transition bg-slate-100 text-slate-700 hover:bg-slate-200"
              >
                {saving ? "Saving..." : saved ? "Saved!" : "Save to Disk"}
              </button>
              <button
                onClick={handlePromote}
                disabled={saving}
                className="px-3 py-1.5 rounded-lg text-sm font-medium transition bg-emerald-600 text-white hover:bg-emerald-700"
              >
                Upload to Server
              </button>
              <span className="rounded-full bg-amber-100 text-amber-800 text-xs px-2 py-0.5 font-medium">
                Local
              </span>
            </>
          )}
          {!isReadOnly && !isNew && !isLocal && (
            <button
              onClick={handleSave}
              disabled={saving}
              className="px-3 py-1.5 rounded-lg text-sm font-medium transition bg-blue-600 text-white hover:bg-blue-700"
            >
              {saving ? "Saving..." : saved ? "Saved!" : "Save"}
            </button>
          )}
          {isNew && (
            <button
              onClick={handleSave}
              disabled={saving}
              className="px-3 py-1.5 rounded-lg text-sm font-medium transition bg-blue-600 text-white hover:bg-blue-700"
            >
              {saving ? "Creating..." : "Create"}
            </button>
          )}
          {doc && !isLocal && (
            <>
              <button
                onClick={() => setShowHistory(!showHistory)}
                className="px-3 py-1.5 rounded-lg text-sm font-medium transition bg-slate-100 text-slate-700 hover:bg-slate-200"
              >
                {showHistory ? "Hide History" : "History"}
              </button>
              {isOwner && (
                <button
                  onClick={() => setShowShare(true)}
                  className="px-3 py-1.5 rounded-lg text-sm font-medium transition bg-slate-100 text-slate-700 hover:bg-slate-200"
                >
                  Share
                </button>
              )}
            </>
          )}
          {isReadOnly && (
            <span className="rounded-full bg-red-100 text-red-700 text-xs px-2 py-0.5 font-medium">
              Read-only
            </span>
          )}
          {isConnected ? (
            <span className="rounded-full bg-emerald-100 text-emerald-700 text-xs px-2 py-0.5 font-medium">
              Connected
            </span>
          ) : (
            <span className="rounded-full bg-amber-100 text-amber-700 text-xs px-2 py-0.5 font-medium">
              Offline{queuedCount > 0 ? ` (${queuedCount})` : ""}
            </span>
          )}
          <div className="flex-1" />
          {user && (
            <>
              <span className="text-sm text-slate-500">{user.username}</span>
              <button
                onClick={() => { logout(); navigate({ to: "/login" }); }}
                className="bg-slate-100 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-slate-200 transition"
              >
                Logout
              </button>
            </>
          )}
        </div>
      </div>
      </div>

      <PresenceBar users={activeUsers} />
      {(error || wsError) && (
        <div className="bg-red-50 text-red-600 px-3 py-1.5 text-sm">
          {(error as Error)?.message || wsError}
        </div>
      )}
      <div className="flex flex-1">
        <div className="flex-1">
          {showContent ? (
            <SyncEditor
              value={content}
              version={doc?.version ?? 0}
              onChange={setContent}
              onOperation={handleOperation}
              applyRemoteOperation={(fn) => { applyOpRef.current = fn; }}
              readonly={isReadOnly}
            />
          ) : (
            <div className="p-5 text-slate-400 text-sm">Loading...</div>
          )}
        </div>
        {showHistory && doc && !isLocal && (
          <VersionPanel documentId={doc.id} onRestore={handleRestore} />
        )}
      </div>
      {showShare && doc && !isLocal && (
        <ShareDialog documentId={doc.id} onClose={() => setShowShare(false)} />
      )}
      {showShare && isLocal && id && (
        <ShareDialog documentId={0} isLocal onClose={() => setShowShare(false)} />
      )}
    </div>
  );
}
