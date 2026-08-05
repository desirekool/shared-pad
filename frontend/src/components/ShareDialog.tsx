import { useEffect, useState, useRef, useCallback } from "react";
import { listPermissions, shareDocument, revokePermission, type PermissionInfo } from "../api/documents";
import { searchUsers } from "../api/auth";

interface ShareDialogProps {
  documentId: number;
  onClose: () => void;
  isLocal?: boolean;
}

export default function ShareDialog({ documentId, onClose, isLocal }: ShareDialogProps) {
  const [permissions, setPermissions] = useState<PermissionInfo[]>([]);
  const [username, setUsername] = useState("");
  const [level, setLevel] = useState("EDITOR");
  const [error, setError] = useState("");
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const inputRef = useRef<HTMLInputElement>(null);

  const loadPermissions = () => {
    if (isLocal) return;
    listPermissions(documentId)
      .then(setPermissions)
      .catch((e) => setError(e.message));
  };

  useEffect(() => {
    loadPermissions();
  }, [documentId, isLocal]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (inputRef.current && !inputRef.current.parentElement?.contains(e.target as Node)) {
        setShowSuggestions(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleInputChange = useCallback((value: string) => {
    setUsername(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (value.length < 1) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }
    debounceRef.current = setTimeout(async () => {
      try {
        const results = await searchUsers(value);
        setSuggestions(results);
        setShowSuggestions(results.length > 0);
      } catch {
        setSuggestions([]);
      }
    }, 300);
  }, []);

  const handleShare = async () => {
    setError("");
    if (isLocal) {
      setError("Sharing local files is session-only and resets on tab close.");
      return;
    }
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
      className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-xl shadow-2xl p-6 w-full max-w-md"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-bold text-slate-900 mb-4">Share Document</h2>

        {error && <div className="bg-red-50 text-red-600 px-3 py-2 rounded-lg text-sm mb-3">{error}</div>}

        <div className="relative flex gap-2 mb-4">
          <div className="relative flex-1">
            <input
              ref={inputRef}
              placeholder="Username"
              value={username}
              onChange={(e) => handleInputChange(e.target.value)}
              onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition text-sm"
            />
            {showSuggestions && (
              <div className="absolute w-full bg-white border border-slate-200 rounded-lg shadow-lg mt-1 max-h-40 overflow-y-auto z-10">
                {suggestions.map((name) => (
                  <div
                    key={name}
                    className="px-3 py-2 hover:bg-slate-50 cursor-pointer text-sm transition"
                    onMouseDown={() => {
                      setUsername(name);
                      setShowSuggestions(false);
                    }}
                  >
                    {name}
                  </div>
                ))}
              </div>
            )}
          </div>
          <select
            value={level}
            onChange={(e) => setLevel(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg bg-white text-sm"
          >
            <option value="EDITOR">Editor</option>
            <option value="VIEWER">Viewer</option>
          </select>
          <button
            onClick={handleShare}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition text-sm font-medium"
          >
            Share
          </button>
        </div>

        {isLocal && (
          <p className="text-amber-700 text-xs mb-3 italic">
            Local file sharing is session-only and resets on tab close.
          </p>
        )}

        <h3 className="font-semibold text-sm text-slate-700 mb-2">People with access</h3>
        {permissions.length === 0 ? (
          <p className="text-slate-400 text-sm italic">No shared users</p>
        ) : (
          <ul className="list-none p-0 m-0">
            {permissions.map((perm) => (
              <li
                key={perm.id}
                className="flex justify-between items-center py-2 border-b border-slate-100 last:border-b-0"
              >
                <div>
                  <span className="font-medium text-sm">{perm.username}</span>
                  <span className="ml-2 text-xs text-slate-500">
                    ({perm.permissionLevel})
                  </span>
                </div>
                {perm.permissionLevel !== "OWNER" && (
                  <button
                    onClick={() => handleRevoke(perm)}
                    className="text-red-500 hover:text-red-700 text-sm font-medium transition"
                  >
                    Revoke
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}

        <div className="mt-4 text-right">
          <button
            onClick={onClose}
            className="bg-slate-100 text-slate-700 px-4 py-2 rounded-lg hover:bg-slate-200 transition text-sm font-medium"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
