import { useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import { authLogin, authRegister, ApiError } from "../services/api";
import Card from "../components/Card";
import Input from "../components/Input";
import Btn from "../components/Btn";

type Mode = "login" | "register";

interface FormState {
  name: string;
  email: string;
  password: string;
}

const fieldError = (errors: Record<string, string>, key: string) =>
  errors[key] ? (
    <p style={{ fontSize: "12px", color: "var(--danger)", marginTop: "4px" }}>
      {errors[key]}
    </p>
  ) : null;

export default function AuthPage() {
  const { login } = useAuth();

  const [mode, setMode] = useState<Mode>("login");
  const [form, setForm] = useState<FormState>({
    name: "",
    email: "",
    password: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const set = (key: keyof FormState, value: string) =>
    setForm((f) => ({ ...f, [key]: value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setFieldErrors({});
    setLoading(true);

    try {
      const data =
        mode === "login"
          ? await authLogin({ email: form.email, password: form.password })
          : await authRegister({
              name: form.name,
              email: form.email,
              password: form.password,
            });

      login(data);
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.errors).length > 0) {
        setFieldErrors(err.errors);
      } else {
        setError(err instanceof Error ? err.message : "Erro desconhecido");
      }
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (m: Mode) => {
    setMode(m);
    setError("");
    setFieldErrors({});
    setForm({ name: "", email: "", password: "" });
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background:
          "radial-gradient(ellipse 80% 60% at 50% -10%, rgba(124,106,255,0.15) 0%, transparent 70%)",
        padding: "20px",
      }}
    >
      <div className="fade-in" style={{ width: "100%", maxWidth: "400px" }}>
        <div style={{ textAlign: "center", marginBottom: "40px" }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: "16px",
              margin: "0 auto 16px",
              background:
                "linear-gradient(135deg, var(--accent), var(--accent2))",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "24px",
              boxShadow: "0 8px 32px rgba(124,106,255,0.4)",
            }}
          >
            ⚡
          </div>
          <h1
            style={{
              fontSize: "28px",
              fontWeight: 800,
              letterSpacing: "-0.5px",
            }}
          >
            Storefront
          </h1>
          <p
            style={{
              color: "var(--text2)",
              marginTop: "4px",
              fontSize: "13px",
            }}
          >
            Plataforma de Comércio
          </p>
        </div>

        <div
          style={{
            display: "flex",
            background: "var(--surface)",
            borderRadius: "12px",
            padding: "4px",
            marginBottom: "24px",
            border: "1px solid var(--border)",
          }}
        >
          {(["login", "register"] as Mode[]).map((m) => (
            <button
              key={m}
              onClick={() => switchMode(m)}
              style={{
                flex: 1,
                padding: "8px",
                borderRadius: "8px",
                border: "none",
                background: mode === m ? "var(--surface2)" : "transparent",
                color: mode === m ? "var(--text)" : "var(--text2)",
                fontFamily: "Syne, sans-serif",
                fontWeight: 600,
                fontSize: "13px",
                transition: "all 0.2s",
                cursor: "pointer",
              }}
            >
              {m === "login" ? "Entrar" : "Cadastrar"}
            </button>
          ))}
        </div>

        <Card>
          <form
            onSubmit={handleSubmit}
            style={{ display: "flex", flexDirection: "column", gap: "16px" }}
          >
            {mode === "register" && (
              <div>
                <Input
                  label="Nome completo"
                  placeholder="Seu nome"
                  value={form.name}
                  onChange={(e) => set("name", e.target.value)}
                  required
                />
                {fieldError(fieldErrors, "name")}
              </div>
            )}

            <div>
              <Input
                label="E-mail"
                type="email"
                placeholder="seu@email.com"
                value={form.email}
                onChange={(e) => set("email", e.target.value)}
                required
              />
              {fieldError(fieldErrors, "email")}
            </div>

            <div>
              <Input
                label="Senha"
                type="password"
                placeholder="••••••••"
                value={form.password}
                onChange={(e) => set("password", e.target.value)}
                required
              />
              {fieldError(fieldErrors, "password")}
            </div>

            {error && (
              <div
                style={{
                  background: "rgba(255,68,102,0.1)",
                  border: "1px solid rgba(255,68,102,0.3)",
                  borderRadius: "8px",
                  padding: "10px 14px",
                  fontSize: "13px",
                  color: "var(--danger)",
                }}
              >
                {error}
              </div>
            )}

            <Btn
              type="submit"
              size="lg"
              disabled={loading}
              style={{
                width: "100%",
                justifyContent: "center",
                marginTop: "4px",
              }}
            >
              {loading
                ? "Carregando..."
                : mode === "login"
                  ? "Entrar"
                  : "Criar conta"}
            </Btn>
          </form>
        </Card>
      </div>
    </div>
  );
}
