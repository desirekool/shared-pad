import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listDocuments, deleteDocument, type DocumentResponse } from "../../api/documents";

export default function DocumentList() {
  const navigate = useNavigate();
  const [docs, setDocs] = useState<DocumentResponse[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    listDocuments()
      .then(setDocs)
      .catch((e) => setError(e.message));
  }, []);

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this document?")) return;
    try {
      await deleteDocument(id);
      setDocs((prev) => prev.filter((d) => d.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Delete failed");
    }
  };

  return (
    <div>
      <h2>My Documents</h2>
      {error && <p style={{ color: "red" }}>{error}</p>}
      <button onClick={() => navigate("/docs/new")}>New Document</button>
      {docs.length === 0 ? (
        <p>No documents yet. Create one!</p>
      ) : (
        <ul>
          {docs.map((doc) => (
            <li key={doc.id}>
              <span
                style={{ cursor: "pointer", color: "blue" }}
                onClick={() => navigate(`/docs/${doc.id}`)}
              >
                {doc.title}
              </span>
              <span style={{ marginLeft: 12, color: "#666" }}>
                v{doc.version} - {new Date(doc.updatedAt).toLocaleDateString()}
              </span>
              <button onClick={() => handleDelete(doc.id)} style={{ marginLeft: 8 }}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
