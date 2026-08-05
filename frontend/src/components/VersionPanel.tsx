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
    <div className="p-3 border-l border-slate-300 w-[300px] overflow-y-auto">
      <h3 className="m-0 mb-2 text-base font-semibold">Version History</h3>
      {error && <p className="text-red-500 text-xs">{error}</p>}
      <ul className="list-none p-0 m-0">
        {versions.map((v) => (
          <li
            key={v.id}
            className={`px-2 py-1.5 cursor-pointer rounded mb-0.5 ${selectedVersion === v.versionNumber ? "bg-blue-50" : "bg-transparent hover:bg-slate-50"}`}
            onClick={() => handlePreview(v.versionNumber)}
          >
            <div className="font-medium text-[13px]">{v.message}</div>
            <div className="text-[11px] text-slate-500">
              {v.createdBy} - {new Date(v.createdAt).toLocaleString()}
            </div>
          </li>
        ))}
      </ul>
      {previewContent && (
        <div className="mt-3">
          <h4 className="m-0 mb-1 text-[13px] font-medium">Preview (v{selectedVersion})</h4>
          <pre className="text-[11px] max-h-[200px] overflow-auto bg-slate-100 p-2 rounded whitespace-pre-wrap break-all">
            {previewContent.slice(0, 2000)}
            {previewContent.length > 2000 ? "..." : ""}
          </pre>
          <button onClick={handleRestore} className="mt-1 text-xs text-blue-600 hover:text-blue-800 underline">
            Restore this version
          </button>
        </div>
      )}
    </div>
  );
}
