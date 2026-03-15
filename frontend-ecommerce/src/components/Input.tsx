import type { InputHTMLAttributes } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export default function Input({ label, error, ...rest }: InputProps) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
      {label && (
        <label
          style={{
            fontSize: "12px",
            color: "var(--text2)",
            fontWeight: 500,
            letterSpacing: "0.04em",
          }}
        >
          {label}
        </label>
      )}

      <input {...rest} />

      {error && (
        <span style={{ fontSize: "11px", color: "var(--danger)" }}>
          {error}
        </span>
      )}
    </div>
  );
}
