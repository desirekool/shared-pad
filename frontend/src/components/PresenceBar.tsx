import type { RemoteUser } from "../hooks/usePresence";

interface PresenceBarProps {
  users: RemoteUser[];
}

export default function PresenceBar({ users }: PresenceBarProps) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: 8,
        padding: "4px 12px",
        borderBottom: "1px solid #ccc",
        fontSize: 13,
      }}
    >
      <span style={{ color: "#666" }}>Active:</span>
      {users.length === 0 ? (
        <span style={{ color: "#999" }}>You're the only one here</span>
      ) : (
        users.map((user) => (
          <div
            key={user.userId}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 4,
              padding: "2px 8px",
              background: "#e8f4fd",
              borderRadius: 12,
            }}
          >
            <span
              style={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                background: user.typing ? "#f0ad4e" : "#5cb85c",
                display: "inline-block",
              }}
            />
            <span>{user.username}</span>
            {user.typing && (
              <span style={{ color: "#999", fontStyle: "italic" }}>typing...</span>
            )}
          </div>
        ))
      )}
    </div>
  );
}
