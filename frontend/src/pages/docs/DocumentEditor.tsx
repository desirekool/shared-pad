import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getDocument, updateDocument, type DocumentResponse } from "../../api/documents";
import SyncEditor, { type EditorOperation } from "../../components/SyncEditor";
import { useDocumentSync } from "../../hooks/useDocumentSync";
import { usePresence } from "../../hooks/usePresence";
import PresenceBar from "../../components/PresenceBar";
import VersionPanel from "../../components/VersionPanel";
import ShareDialog from "../../components/ShareDialog";

export default function DocumentEditor() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [doc, setDoc] = useState<DocumentResponse | null>(null);
  const [content, setContent] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [showShare, setShowShare] = useState(false);
  const applyOpRef = useRef<((op: EditorOperation) => void) | null>(null);

  const isOwner = doc?.permissionLevel === "OWNER";
  const isReadOnly = doc?.permissionLevel === "VIEWER";

  const { activeUsers, joinDocument, leaveDocument } = usePresence({
    documentId: id || "new",
  });

  const fetchDoc = useCallback(() => {
    if (!id || id === "new") return;
    getDocument(Number(id))
      .then((d) => {
        setDoc(d);
        setTitle(d.title);
        setContent(d.content || "");
      })
      .catch((e) => setError(e.message));
  }, [id]);

  useEffect(() => {
    fetchDoc();
  }, [fetchDoc]);

  useEffect(() => {
    if (!id || id === "new") return;
    joinDocument(id);
    return () => leaveDocument(id);
  }, [id, joinDocument, leaveDocument]);

  const handleRejected = useCallback(() => {
    setError("Edit rejected due to version conflict. Re-fetching...");
    fetchDoc();
  }, [fetchDoc]);

  const handleRemoteOp = useCallback((op: EditorOperation) => {
    applyOpRef.current?.(op);
  }, []);

  const { sendOperation, setApplyOp } = useDocumentSync({
    documentId: id || "new",
    version: doc?.version || 0,
    onRemoteOperation: handleRemoteOp,
    onRejected: handleRejected,
  });

  useEffect(() => {
    setApplyOp((op: EditorOperation) => {
      applyOpRef.current?.(op);
    });
  }, [setApplyOp]);

  const handleOperation = useCallback(
    (op: EditorOperation) => {
      if (isReadOnly) return;
      sendOperation(op);
    },
    [sendOperation, isReadOnly]
  );

  const handleSave = async () => {
    if (!doc || isReadOnly) return;
    setSaving(true);
    try {
      const updated = await updateDocument(doc.id, { title, content });
      setDoc(updated);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Save failed");
    } finally {
      setSaving(false);
    }
  };

  const handleRestore = useCallback((restoredContent: string) => {
    setContent(restoredContent);
    setShowHistory(false);
  }, []);

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>
      <div style={{ padding: "8px 16px", display: "flex", gap: 8, alignItems: "center", borderBottom: "1px solid #ccc" }}>
        <button onClick={() => navigate("/docs")}>Back</button>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          style={{ fontSize: 16, flex: 1, padding: "4px 8px" }}
          readOnly={isReadOnly}
        />
        {!isReadOnly && (
          <button onClick={handleSave} disabled={saving}>
            {saving ? "Saving..." : "Save"}
          </button>
        )}
        {doc && (
          <>
            <button onClick={() => setShowHistory(!showHistory)}>
              {showHistory ? "Hide History" : "History"}
            </button>
            {isOwner && (
              <button onClick={() => setShowShare(true)}>Share</button>
            )}
          </>
        )}
        {isReadOnly && (
          <span style={{ fontSize: 12, color: "#c00", padding: "2px 8px", background: "#fee", borderRadius: 4 }}>
            Read-only
          </span>
        )}
      </div>
      <PresenceBar users={activeUsers} />
      {error && <div style={{ padding: 8, color: "red" }}>{error}</div>}
      <div style={{ display: "flex", flex: 1 }}>
        <div style={{ flex: 1 }}>
          {id === "new" ? (
            <div style={{ padding: 20 }}>
              <h2>New Document</h2>
              <p>Use the Create button on the document list.</p>
            </div>
          ) : doc ? (
            <SyncEditor
              value={content}
              version={doc.version}
              onChange={setContent}
              onOperation={handleOperation}
              readonly={isReadOnly}
            />
          ) : (
            <div style={{ padding: 20 }}>Loading...</div>
          )}
        </div>
        {showHistory && doc && (
          <VersionPanel documentId={doc.id} onRestore={handleRestore} />
        )}
      </div>
      {showShare && doc && (
        <ShareDialog documentId={doc.id} onClose={() => setShowShare(false)} />
      )}
    </div>
  );
}
