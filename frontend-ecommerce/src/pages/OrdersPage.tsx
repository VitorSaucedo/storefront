import { useState, useEffect, useCallback } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  getAllOrders,
  getOrderById,
  cancelOrder,
  getProducts,
} from "../services/api";
import type { OrderResponse } from "../services/api";
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

export default function OrdersPage() {
  const { token } = useAuth();

  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<OrderResponse | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [toast, setToast] = useState<ToastState | null>(null);
  const [statusFilter, setStatusFilter] = useState("all");
  const [productMap, setProductMap] = useState<Record<string, string>>({});

  const loadData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const data = await getAllOrders(token);
      setOrders(data.content || []);
    } catch (err) {
      console.error("Erro ao carregar pedidos:", err);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadData();

    if (token) {
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
    }
  }, [token, loadData]);

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
      loadData();
    } catch (err) {
      setToast({
        message: err instanceof Error ? err.message : "Erro ao cancelar pedido",
        type: "error",
      });
    } finally {
      setCancelling(false);
    }
  };

  const statuses = ["all", ...Array.from(new Set(orders.map((o) => o.status)))];
  const filtered = orders.filter(
    (o) => statusFilter === "all" || o.status === statusFilter,
  );

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  return (
    <div
      className="fade-in page-container"
      
    >
      <div style={{ marginBottom: "28px" }}>
        <h2
          style={{ fontSize: "26px", fontWeight: 800, letterSpacing: "-0.5px" }}
        >
          Pedidos
        </h2>
        <p
          style={{ color: "var(--text2)", marginTop: "4px", fontSize: "13px" }}
        >
          {orders.length} pedidos no sistema
        </p>
      </div>

      <div
        style={{
          display: "flex",
          gap: "8px",
          marginBottom: "24px",
          flexWrap: "wrap",
        }}
      >
        {statuses.map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            style={{
              padding: "6px 14px",
              borderRadius: "100px",
              border: "1px solid",
              borderColor:
                statusFilter === s ? "var(--accent)" : "var(--border)",
              background:
                statusFilter === s ? "rgba(124,106,255,0.15)" : "transparent",
              color: statusFilter === s ? "var(--accent)" : "var(--text2)",
              fontSize: "12px",
              fontWeight: 500,
              cursor: "pointer",
              transition: "all 0.15s",
            }}
          >
            {s === "all" ? "Todos" : s}
          </button>
        ))}
      </div>

      {loading ? (
        <Spinner />
      ) : filtered.length === 0 ? (
        <Empty icon="◑" text="Nenhum pedido encontrado." />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          {filtered.map((o) => (
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
              <div>
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
                  {o.userEmail} · {formatDate(o.createdAt)}
                </span>
              </div>

              <div
                style={{ display: "flex", alignItems: "center", gap: "12px" }}
              >
                <div style={{ textAlign: "right" }}>
                  <p
                    style={{
                      fontSize: "11px",
                      color: "var(--text3)",
                      marginBottom: "2px",
                    }}
                  >
                    Total
                  </p>
                  <p
                    style={{
                      fontSize: "18px",
                      fontFamily: "Syne, sans-serif",
                      fontWeight: 800,
                      color: "var(--accent3)",
                    }}
                  >
                    R$ {parseFloat(o.totalAmount).toFixed(2)}
                  </p>
                </div>
                {o.status === "PENDING" && (
                  <Btn
                    variant="danger"
                    size="sm"
                    onClick={() => handleCancel(o.id)}
                    disabled={cancelling}
                  >
                    {cancelling ? "..." : "Cancelar"}
                  </Btn>
                )}
                <Btn
                  variant="secondary"
                  size="sm"
                  onClick={() => openDetail(o.id)}
                  disabled={loadingDetail}
                >
                  {loadingDetail ? "Carregando..." : "Detalhes"}
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
                { label: "Usuário", value: selected.userEmail },
                { label: "Criado em", value: formatDate(selected.createdAt) },
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
