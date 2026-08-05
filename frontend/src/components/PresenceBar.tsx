import type { RemoteUser } from "../hooks/usePresence";

interface PresenceBarProps {
  users: RemoteUser[];
}

export default function PresenceBar({ users }: PresenceBarProps) {
  return (
    <div className="flex items-center gap-2 px-3 py-1 border-b border-slate-300 text-[13px]">
      <span className="text-slate-500">Active:</span>
      {users.length === 0 ? (
        <span className="text-slate-400">You're the only one here</span>
      ) : (
        users.map((u) => (
          <div
            key={u.userId}
            className="flex items-center gap-1 px-2 py-0.5 bg-blue-50 rounded-xl"
          >
            <span
              className={`inline-block w-2 h-2 rounded-full ${u.typing ? "bg-amber-400" : "bg-green-500"}`}
            />
            <span>{u.username}</span>
            {u.typing && (
              <span className="text-slate-400 italic">typing...</span>
            )}
          </div>
        ))
      )}
    </div>
  );
}
