import type { ProductResponse } from "../services/api";
import Modal from "./Modal";
import Btn from "./Btn";
import Empty from "./Empty";

interface CartItem {
  product: ProductResponse;
  quantity: number;
}

interface CartModalProps {
  isOpen: boolean;
  onClose: () => void;
  items: CartItem[];
  total: number;
  onUpdateQty: (id: string, delta: number) => void;
  onPlaceOrder: () => void;
  isPlacing: boolean;
}

export default function CartModal({
  isOpen,
  onClose,
  items,
  total,
  onUpdateQty,
  onPlaceOrder,
  isPlacing,
}: CartModalProps) {
  return (
    <Modal open={isOpen} onClose={onClose} title="Seu Carrinho" width={520}>
      {items.length === 0 ? (
        <Empty icon="◎" text="Seu carrinho está vazio." />
      ) : (
        <section aria-label="Itens no carrinho">
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "10px",
              maxHeight: "60vh",
              overflowY: "auto",
            }}
          >
            {items.map((item) => (
              <div
                key={item.product.id}
                style={{
                  display: "flex",
                  alignItems: "center",
                  padding: "12px",
                  background: "var(--surface2)",
                  borderRadius: "10px",
                  gap: "12px",
                }}
              >
                <div style={{ flex: 1 }}>
                  <p style={{ fontSize: "14px", fontWeight: 600 }}>
                    {item.product.name}
                  </p>
                  <p style={{ fontSize: "11px", color: "var(--text3)" }}>
                    R$ {parseFloat(item.product.price).toFixed(2)} cada
                  </p>
                </div>

                <div
                  style={{ display: "flex", alignItems: "center", gap: "12px" }}
                >
                  <Btn
                    variant="secondary"
                    onClick={() => onUpdateQty(item.product.id, -1)}
                    aria-label="Diminuir quantidade"
                    style={{
                      width: "32px",
                      height: "32px",
                      padding: 0,
                      justifyContent: "center",
                    }}
                  >
                    −
                  </Btn>
                  <span
                    style={{
                      fontWeight: 700,
                      minWidth: "20px",
                      textAlign: "center",
                    }}
                  >
                    {item.quantity}
                  </span>
                  <Btn
                    variant="secondary"
                    onClick={() => onUpdateQty(item.product.id, 1)}
                    aria-label="Aumentar quantidade"
                    style={{
                      width: "32px",
                      height: "32px",
                      padding: 0,
                      justifyContent: "center",
                    }}
                  >
                    ＋
                  </Btn>
                </div>
              </div>
            ))}
          </div>

          <footer
            style={{
              marginTop: "20px",
              borderTop: "1px solid var(--border)",
              paddingTop: "16px",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: "20px",
              }}
            >
              <span style={{ fontWeight: 700 }}>Total:</span>
              <span
                style={{
                  fontSize: "20px",
                  fontWeight: 800,
                  color: "var(--accent3)",
                }}
              >
                R$ {total.toFixed(2)}
              </span>
            </div>
            <div style={{ display: "flex", gap: "10px" }}>
              <Btn
                variant="secondary"
                onClick={onClose}
                style={{ flex: 1, justifyContent: "center" }}
              >
                Voltar
              </Btn>
              <Btn
                onClick={onPlaceOrder}
                disabled={isPlacing}
                style={{ flex: 1, justifyContent: "center" }}
              >
                {isPlacing ? "Finalizando..." : "Finalizar Pedido"}
              </Btn>
            </div>
          </footer>
        </section>
      )}
    </Modal>
  );
}
