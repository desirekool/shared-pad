import { Routes, Route, Navigate, Link } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ProtectedRoute from "./components/ProtectedRoute";
import DocumentList from "./pages/docs/DocumentList";
import DocumentEditor from "./pages/docs/DocumentEditor";

function Home() {
  const { user, logout } = useAuth();
  return (
    <div>
      <h1>SyncDocs</h1>
      {user && (
        <div>
          <p>Welcome, {user.username}!</p>
          <nav>
            <Link to="/docs">My Documents</Link> |{" "}
            <button onClick={logout}>Logout</button>
          </nav>
        </div>
      )}
    </div>
  );
}

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <Login />}
      />
      <Route
        path="/register"
        element={isAuthenticated ? <Navigate to="/" replace /> : <Register />}
      />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        }
      />
      <Route
        path="/docs"
        element={
          <ProtectedRoute>
            <DocumentList />
          </ProtectedRoute>
        }
      />
      <Route
        path="/docs/:id"
        element={
          <ProtectedRoute>
            <DocumentEditor />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
