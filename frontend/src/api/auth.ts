const API_BASE = "http://localhost:8080/api";

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  roles: string[];
}

export interface ErrorResponse {
  status: number;
  message: string;
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const error: ErrorResponse = await res.json();
    throw new Error(error.message);
  }
  return res.json();
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  return handleResponse<AuthResponse>(res);
}

export async function register(username: string, email: string, password: string): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, email, password }),
  });
  return handleResponse<AuthResponse>(res);
}
