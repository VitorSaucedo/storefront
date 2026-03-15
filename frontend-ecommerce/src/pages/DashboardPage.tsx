import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { getProducts, getAllOrders } from "../services/api";
import type { ProductResponse, OrderResponse } from "../services/api";
import Card from "../components/Card";
import Spinner from "../components/Spinner";

interface Stat {
  label: string;
  value: number | string;
  icon: string;
  color: string;
}

export default function DashboardPage() {
  const { token } = useAuth();

  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) return;

    setLoading(true);
    Promise.all([
      getProducts(token).catch(() => ({ content: [] })),
      getAllOrders(token).catch(() => ({ content: [] })),
    ]).then(([p, o]) => {
      setProducts((p as any).content || []);
      setOrders((o as any).content || []);
      setLoading(false);
    });
  }, [token]);

  const totalRevenue = (orders || [])
    .filter((o) => o.status === "CONFIRMED")
    .reduce((acc, o) => acc + parseFloat(o.totalAmount), 0);

  const stats: Stat[] = [
    {
      label: "Produtos cadastrados",
      value: products.length,
      icon: "◻",
      color: "var(--accent)",
    },
    {
      label: "Pedidos no sistema",
      value: orders.length,
      icon: "◑",
      color: "var(--accent2)",
    },
    {
      label: "Pedidos confirmados",
      value: orders.filter((o) => o.status === "CONFIRMED").length,
      icon: "◉",
      color: "var(--accent3)",
    },
    {
      label: "Receita confirmada",
      value: `R$ ${totalRevenue.toFixed(2)}`,
      icon: "◎",
      color: "var(--warning)",
    },
  ];

  return (
    <div
      className="fade-in"
      style={{ padding: "32px", maxWidth: "1200px", margin: "0 auto" }}
    >
      <div style={{ marginBottom: "32px" }}>
        <h2
          style={{ fontSize: "26px", fontWeight: 800, letterSpacing: "-0.5px" }}
        >
          Dashboard
        </h2>
        <p
          style={{ color: "var(--text2)", marginTop: "4px", fontSize: "13px" }}
        >
          Visão geral da plataforma
        </p>
      </div>

      {loading ? (
        <Spinner />
      ) : (
        <>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
              gap: "16px",
              marginBottom: "32px",
            }}
          >
            {stats.map((s) => (
              <Card
                key={s.label}
                style={{
                  background:
                    "linear-gradient(135deg, var(--surface) 0%, var(--surface2) 100%)",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "flex-start",
                    justifyContent: "space-between",
                  }}
                >
                  <div>
                    <p
                      style={{
                        fontSize: "11px",
                        color: "var(--text2)",
                        marginBottom: "8px",
                        textTransform: "uppercase",
                        letterSpacing: "0.08em",
                      }}
                    >
                      {s.label}
                    </p>
                    <p
                      style={{
                        fontSize: "36px",
                        fontFamily: "Syne, sans-serif",
                        fontWeight: 800,
                        color: s.color,
                        lineHeight: 1,
                      }}
                    >
                      {s.value}
                    </p>
                  </div>
                  <span
                    style={{ fontSize: "28px", color: s.color, opacity: 0.3 }}
                  >
                    {s.icon}
                  </span>
                </div>
              </Card>
            ))}
          </div>

          <Card>
            <h3
              style={{
                fontFamily: "Syne, sans-serif",
                fontSize: "16px",
                fontWeight: 700,
                marginBottom: "20px",
              }}
            >
              Pedidos recentes
            </h3>

            {orders.length === 0 ? (
              <p style={{ color: "var(--text3)", fontSize: "13px" }}>
                Nenhum pedido encontrado.
              </p>
            ) : (
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                }}
              >
                {orders.slice(0, 5).map((o) => (
                  <div
                    key={o.id}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "12px 16px",
                      background: "var(--surface2)",
                      borderRadius: "10px",
                      border: "1px solid var(--border)",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "2px",
                      }}
                    >
                      <span style={{ fontSize: "13px", fontWeight: 500 }}>
                        Pedido #{o.id}
                      </span>
                      <span style={{ fontSize: "11px", color: "var(--text3)" }}>
                        {o.userEmail}
                      </span>
                    </div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "16px",
                      }}
                    >
                      <span style={{ fontSize: "13px", color: "var(--text2)" }}>
                        R$ {parseFloat(o.totalAmount).toFixed(2)}
                      </span>
                      <span
                        className={`tag ${
                          o.status === "CONFIRMED"
                            ? "tag-green"
                            : o.status === "CANCELLED"
                              ? "tag-red"
                              : "tag-yellow"
                        }`}
                      >
                        {o.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
