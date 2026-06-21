import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getDocument, updateDocument, type DocumentResponse } from "../../api/documents";
import SyncEditor, { type EditorOperation } from "../../components/SyncEditor";
import { useDocumentSync } from "../../hooks/useDocumentSync";
import { usePresence } from "../../hooks/usePresence";
import PresenceBar from "../../components/PresenceBar";

export default function DocumentEditor() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [doc, setDoc] = useState<DocumentResponse | null>(null);
  const [content, setContent] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const applyOpRef = useRef<((op: EditorOperation) => void) | null>(null);

  const { activeUsers, sendCursor, sendTyping, joinDocument, leaveDocument } = usePresence({
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
      sendOperation(op);
    },
    [sendOperation]
  );

  const handleSave = async () => {
    if (!doc) return;
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

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>
      <div style={{ padding: "8px 16px", display: "flex", gap: 8, alignItems: "center", borderBottom: "1px solid #ccc" }}>
        <button onClick={() => navigate("/docs")}>Back</button>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          style={{ fontSize: 16, flex: 1, padding: "4px 8px" }}
        />
        <button onClick={handleSave} disabled={saving}>
          {saving ? "Saving..." : "Save"}
        </button>
      </div>
      <PresenceBar users={activeUsers} />
      {error && <div style={{ padding: 8, color: "red" }}>{error}</div>}
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
          />
        ) : (
          <div style={{ padding: 20 }}>Loading...</div>
        )}
      </div>
    </div>
  );
}
