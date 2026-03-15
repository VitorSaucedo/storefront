import type { ButtonHTMLAttributes } from "react";
import type React from "react";

// ─── Types ────────────────────────────────────────────────────────────────────

type Variant = "primary" | "secondary" | "danger" | "ghost" | "success";
type Size = "sm" | "md" | "lg";

interface BtnProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  isLoading?: boolean; // Adicionado para feedback visual
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const variants: Record<Variant, React.CSSProperties> = {
  primary: { background: "var(--accent)", color: "#fff", border: "none" },
  secondary: {
    background: "var(--surface2)",
    color: "var(--text)",
    border: "1px solid var(--border)",
  },
  danger: {
    background: "rgba(255,68,102,0.15)",
    color: "var(--danger)",
    border: "1px solid rgba(255,68,102,0.3)",
  },
  ghost: {
    background: "transparent",
    color: "var(--text2)",
    border: "1px solid var(--border)",
  },
  success: {
    background: "rgba(68,255,170,0.15)",
    color: "var(--accent3)",
    border: "1px solid rgba(68,255,170,0.3)",
  },
};

const sizes: Record<Size, React.CSSProperties> = {
  // Aumentamos o padding/height para garantir a zona de toque (Touch Target)
  sm: {
    padding: "8px 16px",
    fontSize: "12px",
    borderRadius: "8px",
    minHeight: "36px",
  },
  md: {
    padding: "12px 24px",
    fontSize: "14px",
    borderRadius: "10px",
    minHeight: "44px",
  },
  lg: {
    padding: "16px 32px",
    fontSize: "16px",
    borderRadius: "12px",
    fontWeight: 600,
    minHeight: "52px",
  },
};

// ─── Component ────────────────────────────────────────────────────────────────

export default function Btn({
  children,
  variant = "primary",
  size = "md",
  disabled,
  isLoading,
  style,
  ...rest
}: BtnProps) {
  const isDisabled = disabled || isLoading;

  return (
    <button
      disabled={isDisabled}
      // ARIA: Indica se o botão está em estado de carregamento
      aria-busy={isLoading}
      style={{
        ...variants[variant],
        ...sizes[size],
        fontWeight: 500,
        fontFamily: "DM Sans, sans-serif",
        transition: "all 0.2s ease-in-out",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "8px",
        opacity: isDisabled ? 0.6 : 1,
        cursor: isDisabled ? "not-allowed" : "pointer",
        // Evita que o texto quebre em botões pequenos
        whiteSpace: "nowrap",
        userSelect: "none",
        ...style,
      }}
      // Usando pseudo-classes de CSS via JS ou apenas garantindo o hover limpo
      onMouseEnter={(e) => {
        if (!isDisabled) e.currentTarget.style.filter = "brightness(1.1)";
      }}
      onMouseLeave={(e) => {
        if (!isDisabled) e.currentTarget.style.filter = "brightness(1)";
      }}
      onMouseDown={(e) => {
        if (!isDisabled) e.currentTarget.style.transform = "scale(0.97)";
      }}
      onMouseUp={(e) => {
        if (!isDisabled) e.currentTarget.style.transform = "scale(1)";
      }}
      {...rest}
    >
      {isLoading ? (
        <>
          <span
            style={{
              width: "14px",
              height: "14px",
              border: "2px solid currentColor",
              borderTopColor: "transparent",
              borderRadius: "50%",
              animation: "spin 0.8s linear infinite",
            }}
          />
          Carregando...
        </>
      ) : (
        children
      )}
    </button>
  );
}
