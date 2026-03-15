import { lazy, Suspense } from "react"; // Adicionado Suspense
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useNavigate,
} from "react-router-dom";
import { AuthProvider, useAuth } from "./contexts/AuthContext";
import ErrorBoundary from "./components/ErrorBoundary";
import Sidebar from "./components/Sidebar";

// Lazy Imports (Mantenha assim, é ótimo para performance)
const AuthPage = lazy(() => import("./pages/AuthPage"));
const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const ProductsPage = lazy(() => import("./pages/ProductsPage"));
const StorePage = lazy(() => import("./pages/StorePage"));
const OrdersPage = lazy(() => import("./pages/OrdersPage"));
const MyOrdersPage = lazy(() => import("./pages/MyOrdersPage"));
const PaymentsPage = lazy(() => import("./pages/PaymentsPage"));

// 1. Redireciona para a loja se o usuário já estiver logado (evita ficar preso no login)
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { user, initializing } = useAuth();
  if (initializing) return <LoadingScreen />;
  return user ? <Navigate to="/store" replace /> : <>{children}</>;
}

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { user, initializing } = useAuth();
  if (initializing) return <LoadingScreen />;
  return user ? <>{children}</> : <Navigate to="/login" replace />;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAdmin } = useAuth();
  return isAdmin ? <>{children}</> : <Navigate to="/store" replace />;
}

function LoadingScreen() {
  return (
    <div className="loading-screen">
      <div className="spinner" />
    </div>
  );
}

function AppLayout() {
  const navigate = useNavigate();

  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        {/* CORREÇÃO: O Suspense deve envolver as rotas lazy para que a troca de página funcione */}
        <Suspense fallback={<LoadingScreen />}>
          <Routes>
            <Route
              path="/store"
              element={
                <StorePage onOrderPlaced={() => navigate("/my-orders")} />
              }
            />
            <Route path="/my-orders" element={<MyOrdersPage />} />
            <Route path="/my-payments" element={<PaymentsPage />} />

            <Route
              path="/dashboard"
              element={
                <AdminRoute>
                  <DashboardPage />
                </AdminRoute>
              }
            />
            <Route
              path="/products"
              element={
                <AdminRoute>
                  <ProductsPage />
                </AdminRoute>
              }
            />
            <Route
              path="/orders"
              element={
                <AdminRoute>
                  <OrdersPage />
                </AdminRoute>
              }
            />
            <Route
              path="/payments"
              element={
                <AdminRoute>
                  <PaymentsPage />
                </AdminRoute>
              }
            />

            {/* Redirecionamento caso a rota não exista dentro do layout */}
            <Route path="*" element={<Navigate to="/store" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* CORREÇÃO: PublicRoute para não ficar preso no login */}
            <Route
              path="/login"
              element={
                <PublicRoute>
                  <Suspense fallback={<LoadingScreen />}>
                    <AuthPage />
                  </Suspense>
                </PublicRoute>
              }
            />

            <Route
              path="/*"
              element={
                <PrivateRoute>
                  <AppLayout />
                </PrivateRoute>
              }
            />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  );
}
