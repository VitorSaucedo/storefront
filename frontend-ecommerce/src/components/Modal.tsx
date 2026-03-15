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

  // 1. Controle de Scroll e Teclado (ESC)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };

    if (open) {
      document.body.style.overflow = "hidden";
      window.addEventListener("keydown", handleKeyDown);

      // Foco automático para acessibilidade ao abrir
      modalRef.current?.focus();
    }

    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open, onClose]);

  if (!open) return null;

  return createPortal(
    <div
      role="presentation" // Indica que o overlay é decorativo para cliques
      onClick={onClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.8)", // Um pouco mais escuro para melhor contraste
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 9000,
        backdropFilter: "blur(8px)",
        WebkitBackdropFilter: "blur(8px)",
        padding: "16px", // Padding menor para aproveitar tela no mobile
        pointerEvents: "all",
      }}
    >
      <div
        ref={modalRef}
        className="slide-in"
        onClick={(e) => e.stopPropagation()}
        tabIndex={-1} // Permite que o div receba foco programático
        role="dialog" // ARIA: Identifica como um diálogo
        aria-modal="true" // ARIA: Indica que o conteúdo atrás é inerte
        aria-labelledby="modal-title"
        style={{
          background: "var(--surface)",
          border: "1px solid var(--border)",
          borderRadius: "var(--r, 16px)",
          padding: "clamp(16px, 5vw, 28px)", // Padding responsivo
          width: "100%",
          maxWidth: width,
          maxHeight: "90vh", // Aumentado para 90% para caber mais conteúdo no mobile
          overflowY: "auto",
          position: "relative",
          boxShadow: "0 24px 80px rgba(0,0,0,0.6)",
          outline: "none",
        }}
      >
        {/* Header */}
        <div
          style={{
            display: "flex",
            alignItems: "flex-start", // Melhor para títulos longos
            justifyContent: "space-between",
            marginBottom: "24px",
            gap: "12px",
          }}
        >
          <h3
            id="modal-title"
            style={{
              fontFamily: "Syne, sans-serif",
              fontSize: "clamp(16px, 4vw, 20px)", // Fonte responsiva
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
              padding: "8px 12px", // Área de clique maior para touch
              borderRadius: "8px",
              transition: "all 0.15s",
              flexShrink: 0,
            }}
          >
            ×
          </button>
        </div>

        {/* Content */}
        <div style={{ color: "var(--text)" }}>{children}</div>
      </div>
    </div>,
    document.getElementById("modal-root")!,
  );
}
