import { useState, useEffect, useCallback } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  ApiError,
} from "../services/api";
import type { ProductResponse, ProductRequest } from "../services/api";
import Card from "../components/Card";
import Btn from "../components/Btn";
import Input from "../components/Input";
import Modal from "../components/Modal";
import Spinner from "../components/Spinner";
import Empty from "../components/Empty";
import Toast from "../components/Toast";

interface ToastState {
  message: string;
  type: "success" | "error" | "info";
}

interface FormState {
  name: string;
  description: string;
  price: string;
  stockQuantity: string;
  category: string;
  imageUrl: string;
}

const emptyForm: FormState = {
  name: "",
  description: "",
  price: "",
  stockQuantity: "",
  category: "",
  imageUrl: "",
};

const fieldError = (errors: Record<string, string>, key: string) =>
  errors[key] ? (
    <p style={{ fontSize: "12px", color: "var(--danger)", marginTop: "4px" }}>
      {errors[key]}
    </p>
  ) : null;

export default function ProductsPage() {
  const { token } = useAuth();

  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastState | null>(null);
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const [modal, setModal] = useState<null | "create" | ProductResponse>(null);
  const [form, setForm] = useState<FormState>(emptyForm);

  const load = useCallback(() => {
    if (!token) return;
    setLoading(true);
    getProducts(token)
      .then((res) => setProducts(res.content || []))
      .catch(() => {
        setProducts([]);
        setToast({
          message: "Erro ao carregar a lista de produtos",
          type: "error",
        });
      })
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    load();
  }, [load]);

  const set = (key: keyof FormState, value: string) =>
    setForm((f) => ({ ...f, [key]: value }));

  const openCreate = () => {
    setForm(emptyForm);
    setFieldErrors({});
    setModal("create");
  };

  const openEdit = (p: ProductResponse) => {
    setForm({
      name: p.name,
      description: p.description ?? "",
      price: String(p.price),
      stockQuantity: String(p.stockQuantity),
      category: p.category,
      imageUrl: p.imageUrl ?? "",
    });
    setFieldErrors({});
    setModal(p);
  };

  const handleSave = async () => {
    if (!token) return;
    setSaving(true);
    setFieldErrors({});

    const body: ProductRequest = {
      name: form.name,
      description: form.description,
      price: parseFloat(form.price),
      stockQuantity: parseInt(form.stockQuantity),
      category: form.category,
      imageUrl: form.imageUrl,
    };

    try {
      if (modal === "create") {
        await createProduct(body, token);
        setToast({ message: "Produto criado com sucesso!", type: "success" });
      } else if (modal && typeof modal === "object") {
        await updateProduct(modal.id, body, token);
        setToast({ message: "Produto atualizado!", type: "success" });
      }
      setModal(null);
      load();
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.errors).length > 0) {
        setFieldErrors(err.errors);
      } else {
        setToast({
          message: err instanceof Error ? err.message : "Erro ao salvar",
          type: "error",
        });
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!token) return;
    if (!confirm("Deseja deletar este produto?")) return;

    try {
      await deleteProduct(id, token);
      setToast({ message: "Produto removido.", type: "success" });
      load();
    } catch (err) {
      setToast({
        message: err instanceof Error ? err.message : "Erro ao deletar",
        type: "error",
      });
    }
  };

  const isEditing = modal !== null && modal !== "create";

  return (
    <div className="fade-in page-container">
      <div
        style={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
          marginBottom: "28px",
        }}
      >
        <div>
          <h2
            style={{
              fontSize: "26px",
              fontWeight: 800,
              letterSpacing: "-0.5px",
            }}
          >
            Produtos
          </h2>
          <p
            style={{
              color: "var(--text2)",
              marginTop: "4px",
              fontSize: "13px",
            }}
          >
            {products.length} produtos cadastrados
          </p>
        </div>
        <Btn onClick={openCreate}>＋ Novo produto</Btn>
      </div>

      {loading ? (
        <Spinner />
      ) : products.length === 0 ? (
        <Empty icon="◻" text="Nenhum produto cadastrado." />
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
            gap: "14px",
          }}
        >
          {products.map((p) => (
            <Card key={p.id}>
              {p.imageUrl && (
                <div
                  style={{
                    width: "100%",
                    height: 160,
                    borderRadius: "8px",
                    marginBottom: "12px",
                    overflow: "hidden",
                    background: "var(--surface2)",
                  }}
                >
                  <img
                    src={p.imageUrl}
                    alt={p.name}
                    style={{
                      width: "100%",
                      height: "100%",
                      objectFit: "cover",
                    }}
                    onError={(e) => {
                      e.currentTarget.style.display = "none";
                    }}
                  />
                </div>
              )}

              <div style={{ marginBottom: "12px" }}>
                <span className="tag tag-purple">{p.category}</span>
              </div>

              <h3
                style={{
                  fontFamily: "Syne, sans-serif",
                  fontSize: "16px",
                  fontWeight: 700,
                  marginBottom: "6px",
                }}
              >
                {p.name}
              </h3>

              {p.description && (
                <p
                  style={{
                    fontSize: "12px",
                    color: "var(--text2)",
                    marginBottom: "16px",
                    lineHeight: 1.5,
                    display: "-webkit-box",
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: "vertical",
                    overflow: "hidden",
                  }}
                >
                  {p.description}
                </p>
              )}

              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  flexWrap: "wrap",
                  gap: "8px",
                  marginBottom: "16px",
                }}
              >
                <span
                  style={{
                    fontSize: "20px",
                    fontFamily: "Syne, sans-serif",
                    fontWeight: 800,
                    color: "var(--accent3)",
                  }}
                >
                  R$ {parseFloat(p.price).toFixed(2)}
                </span>
                <span
                  className={`tag ${p.stockQuantity > 0 ? "tag-green" : "tag-red"}`}
                >
                  {p.stockQuantity > 0
                    ? `${p.stockQuantity} em estoque`
                    : "Sem estoque"}
                </span>
              </div>

              <div style={{ display: "flex", gap: "8px" }}>
                <Btn
                  variant="secondary"
                  size="sm"
                  onClick={() => openEdit(p)}
                  style={{ flex: 1, justifyContent: "center" }}
                >
                  Editar
                </Btn>
                <Btn
                  variant="danger"
                  size="sm"
                  onClick={() => handleDelete(p.id)}
                  style={{ flex: 1, justifyContent: "center" }}
                >
                  Deletar
                </Btn>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={modal !== null}
        onClose={() => {
          setModal(null);
          setFieldErrors({});
        }}
        title={isEditing ? "Editar produto" : "Novo produto"}
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          <div>
            <Input
              label="Nome"
              placeholder="Nome do produto"
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
            />
            {fieldError(fieldErrors, "name")}
          </div>

          <Input
            label="Descrição"
            placeholder="Descrição opcional"
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
          />

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: "12px",
            }}
          >
            <div>
              <Input
                label="Preço (R$)"
                type="number"
                placeholder="0.00"
                min="0.01"
                step="0.01"
                value={form.price}
                onChange={(e) => set("price", e.target.value)}
              />
              {fieldError(fieldErrors, "price")}
            </div>
            <div>
              <Input
                label="Estoque"
                type="number"
                placeholder="0"
                min="0"
                value={form.stockQuantity}
                onChange={(e) => set("stockQuantity", e.target.value)}
              />
              {fieldError(fieldErrors, "stockQuantity")}
            </div>
          </div>

          <div>
            <Input
              label="Categoria"
              placeholder="Ex: Eletrônicos"
              value={form.category}
              onChange={(e) => set("category", e.target.value)}
            />
            {fieldError(fieldErrors, "category")}
          </div>

          <Input
            label="URL da imagem"
            placeholder="https://exemplo.com/imagem.jpg"
            value={form.imageUrl}
            onChange={(e) => set("imageUrl", e.target.value)}
          />

          <div style={{ display: "flex", gap: "10px", marginTop: "8px" }}>
            <Btn
              variant="secondary"
              onClick={() => {
                setModal(null);
                setFieldErrors({});
              }}
              style={{ flex: 1, justifyContent: "center" }}
            >
              Cancelar
            </Btn>
            <Btn
              onClick={handleSave}
              disabled={saving}
              style={{ flex: 1, justifyContent: "center" }}
            >
              {saving
                ? "Salvando..."
                : isEditing
                  ? "Salvar alterações"
                  : "Criar produto"}
            </Btn>
          </div>
        </div>
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