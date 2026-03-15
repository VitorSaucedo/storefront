import { useState, useEffect } from "react";
import {
  getMyPayments,
  getPaymentsByUser,
  getPaymentByOrder,
  getUsers,
} from "../services/api";
import type { PaymentResponse, UserResponse } from "../services/api";
import { useAuth } from "../contexts/AuthContext";
import Card from "../components/Card";
import Btn from "../components/Btn";
import Spinner from "../components/Spinner";
import Empty from "../components/Empty";
import StatusBadge from "../components/StatusBadge";
import Modal from "../components/Modal";
import Toast from "../components/Toast";

interface ToastState {
  message: string;
  type: "success" | "error" | "info";
}

export default function PaymentsPage() {
  const { token, isAdmin, user } = useAuth();

  const [payments, setPayments] = useState<PaymentResponse[]>([]);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | "">("");
  const [orderId, setOrderId] = useState("");
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<PaymentResponse | null>(null);
  const [toast, setToast] = useState<ToastState | null>(null);

  useEffect(() => {
    if (!token || isAdmin) return;
    setLoading(true);
    getMyPayments(token)
      .then((res) => setPayments(res.content || []))
      .catch((err) => setToast({ message: err.message, type: "error" }))
      .finally(() => setLoading(false));
  }, [token, isAdmin]);

  useEffect(() => {
    if (!token || !isAdmin) return;
    getUsers(token)
      .then((res) => setUsers(res.content || []))
      .catch(() => {});
  }, [token, isAdmin]);

  const handleSearchByUser = async () => {
    if (!token || selectedUserId === "") return;
    setLoading(true);
    try {
      const data = await getPaymentsByUser(selectedUserId, token);
      const content = data.content || [];
      setPayments(content);

      if (content.length === 0) {
        setToast({
          message: "Nenhum pagamento encontrado para este usuário",
          type: "info",
        });
      }
    } catch (err) {
      setToast({
        message:
          err instanceof Error ? err.message : "Erro ao buscar pagamentos",
        type: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSearchByOrder = async () => {
    if (!token || !orderId.trim()) return;
    setLoading(true);
    try {
      const data = await getPaymentByOrder(orderId, token);
      setSelected(data);
    } catch (err) {
      setToast({
        message:
          err instanceof Error ? err.message : "Pagamento não encontrado",
        type: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="page-container"
      style={{ padding: "32px", maxWidth: "1200px", margin: "0 auto" }}
    >
      <div style={{ marginBottom: "32px" }}>
        <h1
          style={{
            fontFamily: "Syne, sans-serif",
            fontSize: "28px",
            fontWeight: 800,
            marginBottom: "8px",
          }}
        >
          Pagamentos
        </h1>
        <p style={{ color: "var(--text2)", fontSize: "14px" }}>
          {isAdmin
            ? "Consulte pagamentos por usuário ou pedido"
            : "Seus pagamentos"}
        </p>
      </div>

      <div
        style={{
          display: "flex",
          gap: "16px",
          marginBottom: "32px",
          flexWrap: "wrap",
        }}
      >
        {isAdmin && (
          <div style={{ display: "flex", gap: "8px", flex: 1, minWidth: 280 }}>
            <select
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
              style={{
                flex: 1,
                background: "var(--surface)",
                border: "1px solid var(--border)",
                borderRadius: "10px",
                color: "var(--text)",
                padding: "10px 14px",
                fontSize: "13px",
                outline: "none",
                cursor: "pointer",
              }}
            >
              <option value="">Selecione um usuário...</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name} — {u.email}
                </option>
              ))}
            </select>
            <Btn
              onClick={handleSearchByUser}
              disabled={selectedUserId === "" || loading}
            >
              Buscar
            </Btn>
          </div>
        )}

        <div style={{ display: "flex", gap: "8px", flex: 1, minWidth: 280 }}>
          <input
            value={orderId}
            onChange={(e) => setOrderId(e.target.value)}
            placeholder="Buscar por ID do pedido..."
            style={{
              flex: 1,
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: "10px",
              color: "var(--text)",
              padding: "10px 14px",
              fontSize: "13px",
              outline: "none",
            }}
            onKeyDown={(e) => e.key === "Enter" && handleSearchByOrder()}
          />
          <Btn
            onClick={handleSearchByOrder}
            disabled={!orderId.trim() || loading}
          >
            Buscar
          </Btn>
        </div>
      </div>

      {loading ? (
        <Spinner />
      ) : payments.length === 0 ? (
        <Empty
          icon="💳"
          text={
            isAdmin
              ? "Selecione um usuário para ver os pagamentos"
              : "Nenhum pagamento encontrado"
          }
        />
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
            gap: "16px",
          }}
        >
          {payments.map((p) => (
            <Card
              key={p.id}
              onClick={() => setSelected(p)}
              style={{ cursor: "pointer" }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "flex-start",
                  marginBottom: "16px",
                }}
              >
                <div>
                  <p
                    style={{
                      fontSize: "11px",
                      color: "var(--text3)",
                      marginBottom: "4px",
                    }}
                  >
                    Pagamento #{p.id}
                  </p>
                  <p style={{ fontSize: "12px", color: "var(--text2)" }}>
                    Pedido #{p.orderId}
                  </p>
                </div>
                <StatusBadge status={p.status} />
              </div>

              <p
                style={{
                  fontSize: "28px",
                  fontFamily: "Syne, sans-serif",
                  fontWeight: 800,
                  color:
                    p.status === "PROCESSED"
                      ? "var(--accent3)"
                      : "var(--danger)",
                  marginBottom: "8px",
                }}
              >
                R$ {parseFloat(p.amount).toFixed(2)}
              </p>

              {p.failureReason && (
                <p style={{ fontSize: "12px", color: "var(--danger)" }}>
                  {p.failureReason}
                </p>
              )}
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={selected !== null}
        onClose={() => setSelected(null)}
        title={selected ? `Pagamento #${selected.id}` : "Detalhes"}
      >
        {selected && (
          <div
            style={{ display: "flex", flexDirection: "column", gap: "16px" }}
          >
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "12px",
              }}
            >
              {[
                { label: "Pedido", value: `#${selected.orderId}` },
                { label: "Usuário", value: `#${selected.userId}` },
                {
                  label: "Valor",
                  value: `R$ ${parseFloat(selected.amount).toFixed(2)}`,
                },
                {
                  label: "Status",
                  value: <StatusBadge status={selected.status} />,
                },
              ].map(({ label, value }) => (
                <div
                  key={label}
                  style={{
                    background: "var(--surface2)",
                    borderRadius: "10px",
                    padding: "12px",
                    border: "1px solid var(--border)",
                  }}
                >
                  <p
                    style={{
                      fontSize: "11px",
                      color: "var(--text3)",
                      marginBottom: "6px",
                    }}
                  >
                    {label}
                  </p>
                  <div style={{ fontSize: "14px", fontWeight: 600 }}>
                    {value}
                  </div>
                </div>
              ))}
            </div>

            {selected.failureReason && (
              <div
                style={{
                  background: "rgba(255,80,80,0.08)",
                  border: "1px solid rgba(255,80,80,0.2)",
                  borderRadius: "10px",
                  padding: "12px",
                }}
              >
                <p
                  style={{
                    fontSize: "12px",
                    color: "var(--text3)",
                    marginBottom: "4px",
                  }}
                >
                  Motivo da falha
                </p>
                <p style={{ fontSize: "13px", color: "var(--danger)" }}>
                  {selected.failureReason}
                </p>
              </div>
            )}

            <Btn
              variant="secondary"
              onClick={() => setSelected(null)}
              style={{ justifyContent: "center" }}
            >
              Fechar
            </Btn>
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
