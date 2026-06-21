import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getDocument, updateDocument, type DocumentResponse } from "../../api/documents";

export default function DocumentEditor() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [doc, setDoc] = useState<DocumentResponse | null>(null);
  const [content, setContent] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    if (!id || id === "new") return;
    getDocument(Number(id))
      .then((d) => {
        setDoc(d);
        setTitle(d.title);
        setContent(d.content || "");
      })
      .catch((e) => setError(e.message));
  }, [id]);

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
    <div style={{ padding: 20 }}>
      <button onClick={() => navigate("/docs")}>Back</button>
      {error && <p style={{ color: "red" }}>{error}</p>}
      {id === "new" ? (
        <div>
          <h2>New Document</h2>
          <p>Use the Create button on the document list.</p>
        </div>
      ) : doc ? (
        <div>
          <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{ fontSize: 18, flex: 1 }}
            />
            <button onClick={handleSave} disabled={saving}>
              {saving ? "Saving..." : "Save"}
            </button>
          </div>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            style={{ width: "100%", height: "70vh", fontFamily: "monospace", fontSize: 14 }}
          />
        </div>
      ) : (
        <p>Loading...</p>
      )}
    </div>
  );
}
