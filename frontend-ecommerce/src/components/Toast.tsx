import { useEffect } from "react";
import { createPortal } from "react-dom";

type ToastType = "success" | "error" | "info";

interface ToastProps {
  message: string;
  type?: ToastType;
  onClose: () => void;
}

const colors: Record<ToastType, string> = {
  success: "var(--accent3)",
  error: "var(--danger)",
  info: "var(--accent)",
};

const icons: Record<ToastType, string> = {
  success: "✓",
  error: "✕",
  info: "ℹ",
};

export default function Toast({
  message,
  type = "success",
  onClose,
}: ToastProps) {
  useEffect(() => {
    const t = setTimeout(onClose, 3500);
    return () => clearTimeout(t);
  }, [onClose]);

  return createPortal(
    <div
      className="slide-in"
      style={{
        background: "var(--surface)",
        border: `1px solid ${colors[type]}`,
        borderRadius: "12px",
        padding: "14px 18px",
        maxWidth: "360px",
        minWidth: "280px",
        display: "flex",
        gap: "12px",
        alignItems: "flex-start",
        boxShadow: `0 16px 48px rgba(0,0,0,0.6), 0 0 0 1px ${colors[type]}22`,
        pointerEvents: "all",
      }}
    >
      <span
        style={{
          color: colors[type],
          fontSize: "16px",
          lineHeight: 1.4,
          flexShrink: 0,
        }}
      >
        {icons[type]}
      </span>
      <span
        style={{
          fontSize: "13px",
          color: "var(--text)",
          lineHeight: 1.5,
          flex: 1,
        }}
      >
        {message}
      </span>
      <button
        onClick={onClose}
        style={{
          background: "none",
          border: "none",
          color: "var(--text3)",
          fontSize: "18px",
          lineHeight: 1,
          cursor: "pointer",
          padding: "0",
          flexShrink: 0,
          marginTop: "1px",
        }}
      >
        ×
      </button>
    </div>,
    document.getElementById("toast-root")!,
  );
}
