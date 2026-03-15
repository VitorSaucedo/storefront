import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  getMyOrders,
  getOrderById,
  cancelOrder,
  getProducts,
} from "../services/api";
import type { OrderResponse, ProductResponse } from "../services/api";
import Card from "../components/Card";
import Btn from "../components/Btn";
import Spinner from "../components/Spinner";
import Empty from "../components/Empty";
import Modal from "../components/Modal";
import StatusBadge from "../components/StatusBadge";
import Toast from "../components/Toast";

interface ToastState {
  message: string;
  type: "success" | "error" | "info";
}

export default function MyOrdersPage() {
  const { token } = useAuth();

  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<OrderResponse | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [toast, setToast] = useState<ToastState | null>(null);
  const [productMap, setProductMap] = useState<Record<string, string>>({});

  const load = () => {
    if (!token) return;
    setLoading(true);
    getMyOrders(token)
      .then((res) => setOrders(res.content || []))
      .catch(() => setOrders([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!token) return;

    getMyOrders(token)
      .then((res) => setOrders(res.content || []))
      .catch(() => setOrders([]))
      .finally(() => setLoading(false));

    getProducts(token)
      .then((res) => {
        const map: Record<string, string> = {};
        (res.content || []).forEach((p) => {
          map[p.id] = p.name;
        });
        setProductMap(map);
      })
      .catch(() => {
        console.warn("Não foi possível carregar o mapeamento de produtos.");
      });
  }, [token]);

  const openDetail = async (id: string) => {
    if (!token) return;
    setLoadingDetail(true);
    try {
      const order = await getOrderById(id, token);
      setSelected(order);
    } catch (err) {
      setToast({
        message: err instanceof Error ? err.message : "Erro ao carregar pedido",
        type: "error",
      });
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleCancel = async (id: string) => {
    if (!token) return;
    if (!confirm("Deseja cancelar este pedido?")) return;
    setCancelling(true);
    try {
      await cancelOrder(id, token);
      setToast({ message: "Pedido cancelado com sucesso.", type: "success" });
      setSelected(null);
      load();
    } catch (err) {
      setToast({
        message: err instanceof Error ? err.message : "Erro ao cancelar pedido",
        type: "error",
      });
    } finally {
      setCancelling(false);
    }
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  const totalSpent = (orders || [])
    .filter((o) => o.status === "CONFIRMED")
    .reduce((acc, o) => acc + parseFloat(o.totalAmount), 0);

  return (
    <div
      className="fade-in page-container"
      style={{ padding: "32px", maxWidth: "1200px", margin: "0 auto" }}
    >
      <div style={{ marginBottom: "28px" }}>
        <h2
          style={{ fontSize: "26px", fontWeight: 800, letterSpacing: "-0.5px" }}
        >
          Meus Pedidos
        </h2>
        <p
          style={{ color: "var(--text2)", marginTop: "4px", fontSize: "13px" }}
        >
          {orders.length} pedidos realizados
        </p>
      </div>

      {!loading && orders.length > 0 && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
            gap: "16px",
            marginBottom: "32px",
          }}
        >
          {[
            {
              label: "Total de pedidos",
              value: orders.length,
              color: "var(--accent)",
            },
            {
              label: "Confirmados",
              value: orders.filter((o) => o.status === "CONFIRMED").length,
              color: "var(--accent3)",
            },
            {
              label: "Pendentes",
              value: orders.filter((o) => o.status === "PENDING").length,
              color: "var(--warning)",
            },
            {
              label: "Total gasto",
              value: `R$ ${totalSpent.toFixed(2)}`,
              color: "var(--accent2)",
            },
          ].map((s) => (
            <Card
              key={s.label}
              style={{
                padding: "16px 20px",
                background:
                  "linear-gradient(135deg, var(--surface) 0%, var(--surface2) 100%)",
              }}
            >
              <p
                style={{
                  fontSize: "11px",
                  color: "var(--text2)",
                  marginBottom: "6px",
                  textTransform: "uppercase",
                  letterSpacing: "0.08em",
                }}
              >
                {s.label}
              </p>
              <p
                style={{
                  fontSize: "26px",
                  fontFamily: "Syne, sans-serif",
                  fontWeight: 800,
                  color: s.color,
                  lineHeight: 1,
                }}
              >
                {s.value}
              </p>
            </Card>
          ))}
        </div>
      )}

      {loading ? (
        <Spinner />
      ) : orders.length === 0 ? (
        <Empty icon="◑" text="Você ainda não fez nenhum pedido." />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          {orders.map((o) => (
            <Card
              key={o.id}
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                padding: "16px 20px",
                gap: "16px",
                flexWrap: "wrap",
              }}
            >
              <div
                style={{ display: "flex", flexDirection: "column", gap: "4px" }}
              >
                <div
                  style={{ display: "flex", alignItems: "center", gap: "10px" }}
                >
                  <span
                    style={{
                      fontFamily: "Syne, sans-serif",
                      fontWeight: 700,
                      fontSize: "15px",
                    }}
                  >
                    Pedido #{o.id}
                  </span>
                  <StatusBadge status={o.status} />
                </div>
                <span style={{ fontSize: "12px", color: "var(--text3)" }}>
                  {formatDate(o.createdAt)} · {o.items.length}{" "}
                  {o.items.length === 1 ? "item" : "itens"}
                </span>
              </div>

              <div
                style={{ display: "flex", alignItems: "center", gap: "10px" }}
              >
                <p
                  style={{
                    fontSize: "20px",
                    fontFamily: "Syne, sans-serif",
                    fontWeight: 800,
                    color: "var(--accent3)",
                  }}
                >
                  R$ {parseFloat(o.totalAmount).toFixed(2)}
                </p>
                {o.status === "PENDING" && (
                  <Btn
                    variant="danger"
                    size="sm"
                    onClick={() => handleCancel(o.id)}
                    disabled={cancelling}
                  >
                    Cancelar
                  </Btn>
                )}
                <Btn
                  variant="secondary"
                  size="sm"
                  onClick={() => openDetail(o.id)}
                  disabled={loadingDetail}
                >
                  Ver detalhes
                </Btn>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={selected !== null}
        onClose={() => setSelected(null)}
        title={selected ? `Pedido #${selected.id}` : "Detalhes"}
        width={540}
      >
        {selected && (
          <div
            style={{ display: "flex", flexDirection: "column", gap: "16px" }}
          >
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "10px",
              }}
            >
              {[
                {
                  label: "Status",
                  value: <StatusBadge status={selected.status} />,
                },
                {
                  label: "Total",
                  value: `R$ ${parseFloat(selected.totalAmount).toFixed(2)}`,
                },
                { label: "Criado em", value: formatDate(selected.createdAt) },
                { label: "Atualizado", value: formatDate(selected.updatedAt) },
              ].map(({ label, value }) => (
                <div
                  key={label}
                  style={{
                    background: "var(--surface2)",
                    borderRadius: "10px",
                    padding: "12px 14px",
                    border: "1px solid var(--border)",
                  }}
                >
                  <p
                    style={{
                      fontSize: "11px",
                      color: "var(--text3)",
                      marginBottom: "4px",
                    }}
                  >
                    {label}
                  </p>
                  <div style={{ fontSize: "13px", fontWeight: 500 }}>
                    {value}
                  </div>
                </div>
              ))}
            </div>

            <div>
              <p
                style={{
                  fontSize: "12px",
                  color: "var(--text2)",
                  marginBottom: "10px",
                  textTransform: "uppercase",
                  letterSpacing: "0.06em",
                }}
              >
                Itens do pedido
              </p>
              <div
                style={{ display: "flex", flexDirection: "column", gap: "8px" }}
              >
                {selected.items.map((item, i) => (
                  <div
                    key={i}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      padding: "10px 14px",
                      background: "var(--surface2)",
                      borderRadius: "8px",
                      border: "1px solid var(--border)",
                    }}
                  >
                    <div>
                      <p style={{ fontSize: "13px", fontWeight: 500 }}>
                        {productMap[item.productId] ??
                          `Produto #${item.productId}`}
                      </p>
                      <p style={{ fontSize: "11px", color: "var(--text3)" }}>
                        {item.quantity}x · R${" "}
                        {parseFloat(item.unitPrice).toFixed(2)} cada
                      </p>
                    </div>
                    <span
                      style={{
                        fontSize: "14px",
                        fontWeight: 700,
                        color: "var(--accent3)",
                      }}
                    >
                      R${" "}
                      {(item.quantity * parseFloat(item.unitPrice)).toFixed(2)}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "14px 16px",
                background: "var(--surface2)",
                borderRadius: "10px",
                border: "1px solid var(--border)",
              }}
            >
              <span style={{ fontFamily: "Syne, sans-serif", fontWeight: 700 }}>
                Total
              </span>
              <span
                style={{
                  fontSize: "20px",
                  fontFamily: "Syne, sans-serif",
                  fontWeight: 800,
                  color: "var(--accent3)",
                }}
              >
                R$ {parseFloat(selected.totalAmount).toFixed(2)}
              </span>
            </div>

            <div style={{ display: "flex", gap: "10px" }}>
              {selected.status === "PENDING" && (
                <Btn
                  variant="danger"
                  onClick={() => handleCancel(selected.id)}
                  disabled={cancelling}
                  style={{ flex: 1, justifyContent: "center" }}
                >
                  {cancelling ? "Cancelando..." : "Cancelar pedido"}
                </Btn>
              )}
              <Btn
                variant="secondary"
                onClick={() => setSelected(null)}
                style={{ flex: 1, justifyContent: "center" }}
              >
                Fechar
              </Btn>
            </div>
          </div>
        )}
      </Modal>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
}
