import { useEffect, useState } from "react";
import { getVersions, getVersionContent, restoreVersion, type DocumentVersion } from "../api/documents";

interface VersionPanelProps {
  documentId: number;
  onRestore: (content: string) => void;
}

export default function VersionPanel({ documentId, onRestore }: VersionPanelProps) {
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null);
  const [previewContent, setPreviewContent] = useState<string | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getVersions(documentId)
      .then(setVersions)
      .catch((e) => setError(e.message));
  }, [documentId]);

  const handlePreview = async (version: number) => {
    setSelectedVersion(version);
    try {
      const data = await getVersionContent(documentId, version);
      setPreviewContent(data.content);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load version");
    }
  };

  const handleRestore = async () => {
    if (selectedVersion == null) return;
    if (!confirm(`Restore version ${selectedVersion}? Current content will be replaced.`)) return;
    try {
      const doc = await restoreVersion(documentId, selectedVersion);
      if (doc.content) onRestore(doc.content);
      setVersions(await getVersions(documentId));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Restore failed");
    }
  };

  return (
    <div style={{ padding: 12, borderLeft: "1px solid #ccc", width: 300, overflowY: "auto" }}>
      <h3 style={{ margin: "0 0 8px 0" }}>Version History</h3>
      {error && <p style={{ color: "red", fontSize: 12 }}>{error}</p>}
      <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
        {versions.map((v) => (
          <li
            key={v.id}
            style={{
              padding: "6px 8px",
              cursor: "pointer",
              background: selectedVersion === v.versionNumber ? "#e8f4fd" : "transparent",
              borderRadius: 4,
              marginBottom: 2,
            }}
            onClick={() => handlePreview(v.versionNumber)}
          >
            <div style={{ fontWeight: 500, fontSize: 13 }}>{v.message}</div>
            <div style={{ fontSize: 11, color: "#666" }}>
              {v.createdBy} - {new Date(v.createdAt).toLocaleString()}
            </div>
          </li>
        ))}
      </ul>
      {previewContent && (
        <div style={{ marginTop: 12 }}>
          <h4 style={{ margin: "0 0 4px 0", fontSize: 13 }}>Preview (v{selectedVersion})</h4>
          <pre
            style={{
              fontSize: 11,
              maxHeight: 200,
              overflow: "auto",
              background: "#f5f5f5",
              padding: 8,
              borderRadius: 4,
              whiteSpace: "pre-wrap",
              wordBreak: "break-all",
            }}
          >
            {previewContent.slice(0, 2000)}
            {previewContent.length > 2000 ? "..." : ""}
          </pre>
          <button onClick={handleRestore} style={{ marginTop: 4, fontSize: 12 }}>
            Restore this version
          </button>
        </div>
      )}
    </div>
  );
}
