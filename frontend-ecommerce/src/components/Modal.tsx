import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import type { ReactNode } from "react";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  width?: number;
}

export default function Modal({
  open,
  onClose,
  title,
  children,
  width = 480,
}: ModalProps) {
  const modalRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);

  // Mantém onCloseRef sempre atualizado sem re-executar o efeito
  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    if (!open) return;

    document.body.style.overflow = "hidden";

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onCloseRef.current();
    };
    window.addEventListener("keydown", handleKeyDown);

    // Foca o modal apenas se nenhum input/textarea já estiver focado
    const active = document.activeElement;
    const isInputFocused =
      active instanceof HTMLInputElement ||
      active instanceof HTMLTextAreaElement ||
      active instanceof HTMLSelectElement;

    if (!isInputFocused) {
      modalRef.current?.focus();
    }

    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]); // <-- apenas `open`, sem `onClose`

  if (!open) return null;

  return createPortal(
    <div
      role="presentation"
      onClick={onClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.8)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 9000,
        backdropFilter: "blur(8px)",
        WebkitBackdropFilter: "blur(8px)",
        padding: "16px",
        pointerEvents: "all",
      }}
    >
      <div
        ref={modalRef}
        className="slide-in"
        onClick={(e) => e.stopPropagation()}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        style={{
          background: "var(--surface)",
          border: "1px solid var(--border)",
          borderRadius: "var(--r, 16px)",
          padding: "clamp(16px, 5vw, 28px)",
          width: "100%",
          maxWidth: width,
          maxHeight: "90vh",
          overflowY: "auto",
          position: "relative",
          boxShadow: "0 24px 80px rgba(0,0,0,0.6)",
          outline: "none",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "flex-start",
            justifyContent: "space-between",
            marginBottom: "24px",
            gap: "12px",
          }}
        >
          <h3
            id="modal-title"
            style={{
              fontFamily: "Syne, sans-serif",
              fontSize: "clamp(16px, 4vw, 20px)",
              fontWeight: 700,
              color: "var(--text)",
            }}
          >
            {title}
          </h3>
          <button
            onClick={onClose}
            aria-label="Fechar modal"
            style={{
              background: "var(--surface2)",
              border: "1px solid var(--border)",
              color: "var(--text2)",
              fontSize: "20px",
              lineHeight: 1,
              cursor: "pointer",
              padding: "8px 12px",
              borderRadius: "8px",
              transition: "all 0.15s",
              flexShrink: 0,
            }}
          >
            ×
          </button>
        </div>

        <div style={{ color: "var(--text)" }}>{children}</div>
      </div>
    </div>,
    document.getElementById("modal-root")!,
  );
}