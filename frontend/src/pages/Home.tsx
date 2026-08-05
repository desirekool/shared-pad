import { useNavigate } from "@tanstack/react-router";
import { useAuth } from "../context/AuthContext";

export default function Home() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate({ to: "/login" });
  };

  return (
    <div className="flex flex-col h-screen">
      <div className="flex items-center gap-4 px-4 py-2 border-b bg-white shadow-sm">
        <span className="font-bold text-lg text-slate-900">SyncDocs</span>
        <div className="flex-1" />
        {user && (
          <>
            <span className="text-sm text-slate-500">{user.username}</span>
            <button
              onClick={handleLogout}
              className="bg-slate-100 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-slate-200 transition"
            >
              Logout
            </button>
          </>
        )}
      </div>
      <div className="p-6 max-w-3xl mx-auto w-full">
        <h1 className="text-2xl font-bold text-slate-900 mb-2">SyncDocs</h1>
        <p className="text-slate-600 mb-4">Real-time collaborative document editor.</p>
        <button
          onClick={() => navigate({ to: "/docs" })}
          className="bg-blue-600 text-white px-4 py-1.5 rounded-lg hover:bg-blue-700 transition text-sm font-medium"
        >
          My Documents
        </button>
      </div>
    </div>
  );
}
