interface EmptyProps {
  icon: string;
  text: string;
}

export default function Empty({ icon, text }: EmptyProps) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: "12px",
        padding: "60px 20px",
        color: "var(--text3)",
      }}
    >
      <span style={{ fontSize: "40px" }}>{icon}</span>
      <p style={{ fontSize: "14px" }}>{text}</p>
    </div>
  );
}
