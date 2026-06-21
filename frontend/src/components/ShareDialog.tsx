import { useEffect, useState } from "react";
import { listPermissions, shareDocument, revokePermission, type PermissionInfo } from "../api/documents";

interface ShareDialogProps {
  documentId: number;
  onClose: () => void;
}

export default function ShareDialog({ documentId, onClose }: ShareDialogProps) {
  const [permissions, setPermissions] = useState<PermissionInfo[]>([]);
  const [username, setUsername] = useState("");
  const [level, setLevel] = useState("EDITOR");
  const [error, setError] = useState("");

  const loadPermissions = () => {
    listPermissions(documentId)
      .then(setPermissions)
      .catch((e) => setError(e.message));
  };

  useEffect(() => {
    loadPermissions();
  }, [documentId]);

  const handleShare = async () => {
    setError("");
    try {
      await shareDocument(documentId, username, level);
      setUsername("");
      loadPermissions();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Share failed");
    }
  };

  const handleRevoke = async (perm: PermissionInfo) => {
    if (!confirm(`Revoke access for ${perm.username}?`)) return;
    try {
      await revokePermission(documentId, perm.id);
      loadPermissions();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Revoke failed");
    }
  };

  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.3)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000,
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: "#fff",
          borderRadius: 8,
          padding: 24,
          minWidth: 400,
          maxWidth: 500,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 style={{ margin: "0 0 16px 0 }}>Share Document</h2>

        {error && <p style={{ color: "red", fontSize: 13 }}>{error}</p>}

        <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
          <input
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={{ flex: 1, padding: "6px 8px" }}
          />
          <select value={level} onChange={(e) => setLevel(e.target.value)} style={{ padding: "6px 8px" }}>
            <option value="EDITOR">Editor</option>
            <option value="VIEWER">Viewer</option>
          </select>
          <button onClick={handleShare}>Share</button>
        </div>

        <h3 style={{ margin: "0 0 8px 0", fontSize: 14 }}>People with access</h3>
        {permissions.length === 0 ? (
          <p style={{ color: "#666", fontSize: 13 }}>No shared users</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {permissions.map((perm) => (
              <li
                key={perm.id}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "6px 0",
                  borderBottom: "1px solid #eee",
                }}
              >
                <div>
                  <span style={{ fontWeight: 500 }}>{perm.username}</span>
                  <span style={{ marginLeft: 8, fontSize: 12, color: "#666" }}>
                    ({perm.permissionLevel})
                  </span>
                </div>
                <button
                  onClick={() => handleRevoke(perm)}
                  style={{ fontSize: 12, color: "#c00" }}
                >
                  Revoke
                </button>
              </li>
            ))}
          </ul>
        )}

        <div style={{ marginTop: 16, textAlign: "right" }}>
          <button onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
