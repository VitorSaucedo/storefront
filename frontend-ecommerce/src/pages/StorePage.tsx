import { useState, useEffect, useMemo } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  getAvailableProducts,
  createOrder,
  getPaymentByOrder,
  poll,
} from "../services/api";
import type { ProductResponse, PaymentResponse } from "../services/api";

import ProductCard from "../components/ProductCard";
import CartModal from "../components/CartModal";
import Spinner from "../components/Spinner";
import Empty from "../components/Empty";
import Toast from "../components/Toast";
import Modal from "../components/Modal";
import StatusBadge from "../components/StatusBadge";
import Btn from "../components/Btn";

interface ToastState {
  message: string;
  type: "success" | "error" | "info";
}

export interface CartItem {
  product: ProductResponse;
  quantity: number;
}

interface OrderConfirmState {
  orderId: string;
  payment: PaymentResponse | null;
  loading: boolean;
  attempts: number;
}

export default function StorePage({
  onOrderPlaced,
}: {
  onOrderPlaced: () => void;
}) {
  const { token } = useAuth();

  // ─── Estados de Dados ──────────────────────────────────────────────────────
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastState | null>(null);

  // ─── Estados de Filtro & Busca ─────────────────────────────────────────────
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");

  // ─── Estados de Carrinho & Pedido ──────────────────────────────────────────
  const [cart, setCart] = useState<CartItem[]>(() => {
    const saved = localStorage.getItem("cart_storefront");
    return saved ? JSON.parse(saved) : [];
  });
  const [cartOpen, setCartOpen] = useState(false);
  const [placing, setPlacing] = useState(false);
  const [orderConfirm, setOrderConfirm] = useState<OrderConfirmState | null>(
    null,
  );

  // ─── Efeitos ───────────────────────────────────────────────────────────────

  useEffect(() => {
    if (!token) return;
    setLoading(true);
    getAvailableProducts(token)
      .then((res) => setProducts(res.content))
      .catch(() =>
        setToast({ message: "Erro ao carregar produtos", type: "error" }),
      )
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    localStorage.setItem("cart_storefront", JSON.stringify(cart));
  }, [cart]);

  // ─── Lógica Memoizada ──────────────────────────────────────────────────────
  const categories = useMemo(
    () => ["all", ...Array.from(new Set(products.map((p) => p.category)))],
    [products],
  );

  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      const matchesSearch = p.name
        .toLowerCase()
        .includes(debouncedSearch.toLowerCase());
      const matchesCategory =
        categoryFilter === "all" || p.category === categoryFilter;
      return matchesSearch && matchesCategory;
    });
  }, [products, debouncedSearch, categoryFilter]);

  // ─── Handlers ──────────────────────────────────────────────────────────────
  const addToCart = (product: ProductResponse) => {
    setCart((prev) => {
      const existing = prev.find((i) => i.product.id === product.id);
      if (existing) {
        return prev.map((i) =>
          i.product.id === product.id ? { ...i, quantity: i.quantity + 1 } : i,
        );
      }
      return [...prev, { product, quantity: 1 }];
    });
  };

  const updateQuantity = (productId: string, delta: number) => {
    setCart((prev) =>
      prev
        .map((item) => {
          if (item.product.id === productId) {
            const newQty = item.quantity + delta;
            return newQty > 0 ? { ...item, quantity: newQty } : item;
          }
          return item;
        })
        .filter((item) => item.quantity > 0),
    );
  };

  const removeFromCart = (id: string) => {
    setCart((prev) => prev.filter((i) => i.product.id !== id));
  };

  const handlePlaceOrder = async () => {
    if (!token || cart.length === 0) return;
    setPlacing(true);

    try {
      const order = await createOrder(
        {
          items: cart.map((i) => ({
            productId: i.product.id,
            quantity: i.quantity,
            unitPrice: i.product.price,
          })),
        },
        token,
      );

      setCart([]);
      setCartOpen(false);
      setOrderConfirm({
        orderId: order.id,
        payment: null,
        loading: true,
        attempts: 0,
      });

      const finalPayment = await poll(
        () => getPaymentByOrder(order.id, token),
        (payment) =>
          payment.status === "PROCESSED" || payment.status === "FAILED",
        15,
        2000,
      );

      setOrderConfirm((prev) =>
        prev ? { ...prev, payment: finalPayment, loading: false } : null,
      );
    } catch (err) {
      setOrderConfirm((prev) => (prev ? { ...prev, loading: false } : null));
      setToast({
        message: "Pedido criado. Verifique o pagamento em breve.",
        type: "info",
      });
    } finally {
      setPlacing(false);
    }
  };

  const cartTotal = cart.reduce(
    (acc, i) => acc + parseFloat(i.product.price) * i.quantity,
    0,
  );

  return (
    <main className="fade-in main-container">
      <header className="store-header">
        <div>
          <h2 className="syne-title">Loja</h2>
          <p className="subtitle">
            {filteredProducts.length} produtos encontrados
          </p>
        </div>

        <Btn
          onClick={() => setCartOpen(true)}
          className="relative-btn"
          aria-label="Abrir carrinho"
        >
          ◎ Carrinho
          {cart.length > 0 && (
            <span className="cart-badge">
              {cart.reduce((acc, i) => acc + i.quantity, 0)}
            </span>
          )}
        </Btn>
      </header>

      <nav className="store-nav" aria-label="Filtros de produtos">
        <input
          type="search"
          placeholder="Buscar produtos..."
          className="search-input"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Campo de busca"
        />
        <div className="filter-group">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setCategoryFilter(cat)}
              className={`filter-btn ${categoryFilter === cat ? "active" : ""}`}
            >
              {cat === "all" ? "Todos" : cat}
            </button>
          ))}
        </div>
      </nav>

      <section aria-label="Lista de produtos">
        {loading ? (
          <Spinner />
        ) : filteredProducts.length === 0 ? (
          <Empty icon="◻" text="Nenhum produto encontrado." />
        ) : (
          <div className="product-grid">
            {filteredProducts.map((p) => (
              <ProductCard
                key={p.id}
                product={p}
                quantityInCart={
                  cart.find((i) => i.product.id === p.id)?.quantity ?? 0
                }
                onAddToCart={addToCart}
                onRemoveFromCart={removeFromCart}
              />
            ))}
          </div>
        )}
      </section>

      <CartModal
        isOpen={cartOpen}
        onClose={() => setCartOpen(false)}
        items={cart}
        total={cartTotal}
        onUpdateQty={updateQuantity}
        onPlaceOrder={handlePlaceOrder}
        isPlacing={placing}
      />

      <Modal
        open={orderConfirm !== null}
        onClose={() => !orderConfirm?.loading && setOrderConfirm(null)}
        title="Status do seu Pedido"
      >
        {orderConfirm && (
          <div className="modal-status-content">
            <p>
              Pedido <strong>#{orderConfirm.orderId}</strong> enviado.
            </p>
            {orderConfirm.loading ? (
              <div className="loading-status">
                <Spinner />
                <p>Processando pagamento...</p>
              </div>
            ) : (
              <div className="fade-in">
                <StatusBadge
                  status={orderConfirm.payment?.status ?? "PENDING"}
                />
                {orderConfirm.payment?.status === "FAILED" && (
                  <p className="error-text">
                    {orderConfirm.payment.failureReason}
                  </p>
                )}
                <Btn
                  onClick={() => {
                    setOrderConfirm(null);
                    onOrderPlaced();
                  }}
                  className="full-width-btn"
                >
                  Ir para meus pedidos
                </Btn>
              </div>
            )}
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
    </main>
  );
}
