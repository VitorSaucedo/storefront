export default function Spinner() {
  return (
    <div style={{ display: "flex", justifyContent: "center", padding: "40px" }}>
      <div
        style={{
          width: 32,
          height: 32,
          borderRadius: "50%",
          border: "3px solid var(--border)",
          borderTopColor: "var(--accent)",
          animation: "spin 0.7s linear infinite",
        }}
      />
    </div>
  );
}
