import type { ProductResponse } from "../services/api";
import Card from "./Card";
import Btn from "./Btn";

interface ProductCardProps {
  product: ProductResponse;
  quantityInCart: number;
  onAddToCart: (p: ProductResponse) => void;
  onRemoveFromCart: (id: string) => void;
}

export default function ProductCard({
  product,
  quantityInCart,
  onAddToCart,
  onRemoveFromCart,
}: ProductCardProps) {
  return (
    <article aria-labelledby={`prod-title-${product.id}`}>
      <Card
        style={{ height: "100%", display: "flex", flexDirection: "column" }}
      >
        <div
          className="product-image"
          style={{
            width: "100%",
            borderRadius: "8px",
            marginBottom: "12px",
            overflow: "hidden",
            background: "var(--surface2)",
          }}
        >
          <img
            src={product.imageUrl || "/placeholder-product.png"}
            alt={product.description || product.name}
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
            onError={(e) => {
              e.currentTarget.src =
                "https://placehold.co/400x400/2a2a32/white?text=Sem+Imagem";
            }}
          />
        </div>

        <div style={{ marginBottom: "8px" }}>
          <span className="tag tag-purple">{product.category}</span>
        </div>

        <h3
          id={`prod-title-${product.id}`}
          style={{
            fontFamily: "Syne, sans-serif",
            fontSize: "16px",
            fontWeight: 700,
            marginBottom: "6px",
          }}
        >
          {product.name}
        </h3>

        <p
          style={{
            fontSize: "12px",
            color: "var(--text2)",
            marginBottom: "16px",
            flex: 1,
          }}
        >
          {product.description}
        </p>

        <p
          style={{
            fontSize: "22px",
            fontFamily: "Syne, sans-serif",
            fontWeight: 800,
            color: "var(--accent3)",
            marginBottom: "16px",
          }}
        >
          R$ {parseFloat(product.price).toFixed(2)}
        </p>

        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          <Btn
            onClick={() => onAddToCart(product)}
            aria-label={`Adicionar ${product.name} ao carrinho`}
            style={{ width: "100%", justifyContent: "center", padding: "12px" }}
          >
            {quantityInCart > 0
              ? `Adicionar mais (${quantityInCart})`
              : "Adicionar ao carrinho"}
          </Btn>

          {quantityInCart > 0 && (
            <Btn
              variant="danger"
              size="sm"
              onClick={() => onRemoveFromCart(product.id)}
              aria-label={`Remover ${product.name} do carrinho`}
              style={{ width: "100%", justifyContent: "center" }}
            >
              Remover do carrinho
            </Btn>
          )}
        </div>
      </Card>
    </article>
  );
}
