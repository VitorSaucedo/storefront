import type { ReactNode, CSSProperties } from "react";

interface CardProps {
  children: ReactNode;
  style?: CSSProperties;
  className?: string;
  onClick?: () => void;
}

export default function Card({
  children,
  style,
  className = "",
  onClick,
}: CardProps) {
  return (
    <div
      className={className}
      onClick={onClick}
      style={{
        background: "var(--surface)",
        border: "1px solid var(--border)",
        borderRadius: "var(--r)",
        padding: "24px",
        cursor: onClick ? "pointer" : undefined,
        transition: onClick ? "border-color 0.2s" : undefined,
        ...style,
      }}
      onMouseEnter={(e) => {
        if (onClick) e.currentTarget.style.borderColor = "var(--accent)";
      }}
      onMouseLeave={(e) => {
        if (onClick) e.currentTarget.style.borderColor = "var(--border)";
      }}
    >
      {children}
    </div>
  );
}
