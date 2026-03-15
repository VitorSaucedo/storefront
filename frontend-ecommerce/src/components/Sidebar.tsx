import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

interface NavItem {
  path: string;
  icon: string;
  label: string;
}

const adminItems: NavItem[] = [
  { path: "/dashboard", icon: "◈", label: "Dashboard" },
  { path: "/products", icon: "◻", label: "Produtos" },
  { path: "/orders", icon: "◑", label: "Pedidos" },
  { path: "/payments", icon: "◎", label: "Pagamentos" },
];

const clientItems: NavItem[] = [
  { path: "/store", icon: "◈", label: "Loja" },
  { path: "/my-orders", icon: "◑", label: "Meus Pedidos" },
  { path: "/my-payments", icon: "◎", label: "Meus Pagamentos" },
];

export default function Sidebar() {
  const { user, isAdmin, logout } = useAuth();
  const location = useLocation();

  const items = isAdmin ? adminItems : clientItems;

  return (
    <aside
      className="sidebar-inner"
      style={{
        width: "var(--sidebar-width, 220px)",
        background: "var(--surface)",
        borderRight: "1px solid var(--border)",
        display: "flex",
        flexDirection: "column",
        height: "100vh",
        position: "fixed",
        left: 0,
        top: 0,
        zIndex: 100,
      }}
    >
      {/* Brand */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "10px",
          marginBottom: "32px",
          paddingLeft: "8px",
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: "10px",
            background:
              "linear-gradient(135deg, var(--accent), var(--accent2))",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "14px",
          }}
        >
          ⚡
        </div>
        <span
          style={{
            fontFamily: "Syne, sans-serif",
            fontWeight: 700,
            fontSize: "16px",
          }}
        >
          Storefront
        </span>
      </div>

      <div style={{ paddingLeft: "8px", marginBottom: "20px" }}>
        <span className={`tag ${isAdmin ? "tag-pink" : "tag-purple"}`}>
          {isAdmin ? "Admin" : "Cliente"}
        </span>
      </div>

      <nav
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "4px",
          flex: 1,
        }}
      >
        {items.map((item) => {
          const active = location.pathname === item.path;
          return (
            <Link
              key={item.path}
              to={item.path}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "10px",
                padding: "10px 12px",
                borderRadius: "10px",
                textDecoration: "none",
                borderLeft: active
                  ? "2px solid var(--accent)"
                  : "2px solid transparent",
                background: active ? "rgba(124,106,255,0.15)" : "transparent",
                color: active ? "var(--accent)" : "var(--text2)",
                fontFamily: "DM Sans, sans-serif",
                fontSize: "14px",
                fontWeight: active ? 500 : 400,
                transition: "all 0.15s",
              }}
            >
              <span style={{ fontSize: "16px", opacity: 0.8 }}>
                {item.icon}
              </span>
              {item.label}
            </Link>
          );
        })}
      </nav>

      {/* User Section */}
      <div
        style={{
          borderTop: "1px solid var(--border)",
          paddingTop: "16px",
          display: "flex",
          alignItems: "center",
          gap: "10px",
        }}
      >
        <div
          style={{
            width: 34,
            height: 34,
            borderRadius: "50%",
            background: "var(--accent)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "14px",
            fontWeight: 700,
            flexShrink: 0,
          }}
        >
          {user?.name?.[0]?.toUpperCase()}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div
            style={{
              fontSize: "13px",
              fontWeight: 500,
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
          >
            {user?.name}
          </div>
          <div
            style={{
              fontSize: "11px",
              color: "var(--text3)",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
          >
            {user?.email}
          </div>
        </div>
        <button
          onClick={logout}
          title="Sair"
          style={{
            background: "none",
            border: "none",
            color: "var(--text3)",
            fontSize: "16px",
            cursor: "pointer",
          }}
        >
          ↪
        </button>
      </div>
    </aside>
  );
}
