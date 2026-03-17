const API_BASE = "/api";

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(fn: UnauthorizedHandler) {
  unauthorizedHandler = fn;
}

// ─── Tipagem e Opções ──────────────────────────────────────────────────────────

interface FetchOptions {
  method?: string;
  body?: unknown;
  token?: string;
  signal?: AbortSignal;
}

export class ApiError extends Error {
  readonly errors: Record<string, string>;

  constructor(message: string, errors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
    this.errors = errors;
  }
}

async function apiFetch<T>(
  path: string,
  { method = "GET", body, token, signal }: FetchOptions = {},
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    signal,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401) {
    if (unauthorizedHandler) unauthorizedHandler();
    throw new ApiError("Sessão expirada. Faça login novamente.");
  }

  if (!res.ok) {
    const err = await res
      .json()
      .catch(() => ({ message: "Erro inesperado no servidor", errors: {} }));
    throw new ApiError(
      err.message || `Erro HTTP ${res.status}`,
      err.errors ?? {},
    );
  }

  if (res.status === 204) return null as T;
  return res.json();
}

export async function poll<T>(
  fn: () => Promise<T>,
  validate: (data: T) => boolean,
  maxAttempts = 15,
  interval = 2000,
): Promise<T> {
  let attempts = 0;
  return new Promise((resolve, reject) => {
    const executePoll = async () => {
      attempts++;
      try {
        const result = await fn();
        if (validate(result)) return resolve(result);
        if (attempts >= maxAttempts)
          return reject(new ApiError("Limite de tempo atingido"));
        setTimeout(executePoll, interval);
      } catch (err) {
        if (attempts >= maxAttempts) return reject(err);
        setTimeout(executePoll, interval);
      }
    };
    executePoll();
  });
}

// ─── Types ────────────────────────────────────────────────────────────────────

export interface PagedResponse<T> {
  content: T[];
  page: {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
  };
}

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  name: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: string;
  stockQuantity: number;
  category: string;
  imageUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  category: string;
  imageUrl: string;
}

export interface OrderItemResponse {
  productId: string;
  quantity: number;
  unitPrice: string;
}

export interface OrderResponse {
  id: string;
  userId: string;
  userEmail: string;
  status: string;
  totalAmount: string;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemRequest {
  productId: string;
  quantity: number;
  unitPrice: string;
}

export interface OrderRequest {
  items: OrderItemRequest[];
}

export interface PaymentResponse {
  id: string;
  orderId: string;
  userId: string;
  amount: string;
  status: string;
  failureReason: string | null;
}

// ─── Métodos de API ───────────────────────────────────────────────────────────

// Auth & Users
export const authLogin = (body: LoginRequest) =>
  apiFetch<AuthResponse>("/auth/login", { method: "POST", body });
export const authRegister = (body: RegisterRequest) =>
  apiFetch<AuthResponse>("/auth/register", { method: "POST", body });
export const getUsers = (token: string) =>
  apiFetch<PagedResponse<UserResponse>>("/auth/users", { token });

// Products
export const getProducts = (token: string, signal?: AbortSignal) =>
  apiFetch<PagedResponse<ProductResponse>>("/products", { token, signal });
export const getAvailableProducts = (token: string, signal?: AbortSignal) =>
  apiFetch<PagedResponse<ProductResponse>>("/products/available", {
    token,
    signal,
  });
export const getProductById = (id: string, token: string) =>
  apiFetch<ProductResponse>(`/products/${id}`, { token });
export const getProductsByCategory = (category: string, token: string) =>
  apiFetch<PagedResponse<ProductResponse>>(`/products/category/${category}`, {
    token,
  });
export const createProduct = (body: ProductRequest, token: string) =>
  apiFetch<ProductResponse>("/products", { method: "POST", body, token });
export const updateProduct = (
  id: string,
  body: ProductRequest,
  token: string,
) =>
  apiFetch<ProductResponse>(`/products/${id}`, { method: "PUT", body, token });
export const deleteProduct = (id: string, token: string) =>
  apiFetch<null>(`/products/${id}`, { method: "DELETE", token });

// Orders
export const getMyOrders = (token: string) =>
  apiFetch<PagedResponse<OrderResponse>>("/orders", { token });
export const getAllOrders = (token: string) =>
  apiFetch<PagedResponse<OrderResponse>>("/orders/all", { token });
export const getOrderById = (id: string, token: string) =>
  apiFetch<OrderResponse>(`/orders/${id}`, { token });
export const createOrder = (body: OrderRequest, token: string) =>
  apiFetch<OrderResponse>("/orders", { method: "POST", body, token });
export const cancelOrder = (id: string, token: string) =>
  apiFetch<OrderResponse>(`/orders/${id}/cancel`, { method: "PATCH", token });

// Payments
export const getMyPayments = (token: string) =>
  apiFetch<PagedResponse<PaymentResponse>>("/payments/user", { token });
export const getPaymentsByUser = (userId: string, token: string) =>
  apiFetch<PagedResponse<PaymentResponse>>(`/payments/user/${userId}`, {
    token,
  });
export const getPaymentByOrder = (orderId: string, token: string) =>
  apiFetch<PaymentResponse>(`/payments/order/${orderId}`, { token });
