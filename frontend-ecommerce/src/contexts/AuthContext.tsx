import {
  createContext,
  useContext,
  useState,
  useEffect,
  useMemo,
  useCallback,
} from "react";
import type { ReactNode } from "react";
import type { AuthResponse } from "../services/api.ts";
import { setUnauthorizedHandler } from "../services/api.ts";

// ─── Types ────────────────────────────────────────────────────────────────────

interface User {
  name: string;
  email: string;
  role: string;
  userId: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAdmin: boolean;
  initializing: boolean;
  login: (data: AuthResponse) => void;
  logout: () => void;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

const STORAGE_KEY = "auth";

function extractUserId(token: string): string {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join(""),
    );

    const userId = JSON.parse(jsonPayload).userId;
    return userId != null ? String(userId) : "0";
  } catch (error) {
    console.error("Falha ao decodificar token:", error);
    return "0";
  }
}

const storage = {
  save: (user: User, token: string) =>
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ user, token })),
  load: (): { user: User; token: string } | null => {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  clear: () => localStorage.removeItem(STORAGE_KEY),
};

// ─── Context ──────────────────────────────────────────────────────────────────

const AuthContext = createContext<AuthContextType | null>(null);

// ─── Provider ─────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [initializing, setInitializing] = useState(true);

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    storage.clear();
  }, []);

  useEffect(() => {
    const stored = storage.load();
    if (stored) {
      setUser(stored.user);
      setToken(stored.token);
    }
    setInitializing(false);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
  }, [logout]);

  const login = useCallback((data: AuthResponse) => {
    const userId = extractUserId(data.token);
    const userObj: User = {
      name: data.name,
      email: data.email,
      role: data.role,
      userId,
    };
    setUser(userObj);
    setToken(data.token);
    storage.save(userObj, data.token);
  }, []);

  const authValue = useMemo(
    () => ({
      user,
      token,
      isAdmin: user?.role === "ADMIN",
      initializing,
      login,
      logout,
    }),
    [user, token, initializing, login, logout],
  );

  return (
    <AuthContext.Provider value={authValue}>{children}</AuthContext.Provider>
  );
}

// ─── Hook ─────────────────────────────────────────────────────────────────────

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context)
    throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  return context;
}
