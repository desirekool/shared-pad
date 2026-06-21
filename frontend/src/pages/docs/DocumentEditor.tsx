import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getDocument, updateDocument, type DocumentResponse } from "../../api/documents";
import SyncEditor, { type EditorOperation } from "../../components/SyncEditor";
import { useDocumentSync } from "../../hooks/useDocumentSync";

export default function DocumentEditor() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [doc, setDoc] = useState<DocumentResponse | null>(null);
  const [content, setContent] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const applyOpRef = useRef<((op: EditorOperation) => void) | null>(null);
  const versionRef = useRef(1);

  const handleRemoteOp = useCallback((op: EditorOperation) => {
    applyOpRef.current?.(op);
  }, []);

  const { sendOperation, setApplyOp } = useDocumentSync({
    documentId: id || "new",
    version: doc?.version || 1,
    onRemoteOperation: handleRemoteOp,
  });

  useEffect(() => {
    setApplyOp((op: EditorOperation) => {
      applyOpRef.current?.(op);
    });
  }, [setApplyOp]);

  useEffect(() => {
    if (!id || id === "new") return;
    getDocument(Number(id))
      .then((d) => {
        setDoc(d);
        setTitle(d.title);
        setContent(d.content || "");
        versionRef.current = d.version;
      })
      .catch((e) => setError(e.message));
  }, [id]);

  const handleOperation = useCallback(
    (op: EditorOperation) => {
      versionRef.current += 1;
      sendOperation({ ...op, version: versionRef.current });
    },
    [sendOperation]
  );

  const handleSave = async () => {
    if (!doc) return;
    setSaving(true);
    try {
      const updated = await updateDocument(doc.id, { title, content });
      setDoc(updated);
      versionRef.current = updated.version;
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
