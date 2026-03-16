# Microservices E-commerce

Plataforma de e-commerce construída com arquitetura de microsserviços, comunicação assíncrona via RabbitMQ, autenticação centralizada com JWT e um frontend React moderno.

---

## Sumário

- [Visão Geral](#visao-geral)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Serviços](#servicos)
- [Fluxo de Negócio](#fluxo-de-negocio)
- [Eventos RabbitMQ](#eventos-rabbitmq)
- [API Reference](#api-reference)
- [Testes](#testes)
- [Configuração e Execução](#configuracao-e-execucao)
- [Variáveis de Ambiente](#variaveis-de-ambiente)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Frontend](#frontend)

---

## Visao Geral

O projeto implementa um sistema de e-commerce com as seguintes capacidades:

- Cadastro e autenticação de usuários com JWT
- Catálogo de produtos com controle de estoque
- Criação e gestão de pedidos com validação de estoque em tempo real
- Processamento de pagamentos assíncrono via events
- Saga de compensação para consistência eventual de estoque
- Notificações automáticas por eventos de domínio
- Interface web completa com painel administrativo

---

## Arquitetura

```
                        ┌─────────────────┐
                        │   Frontend      │
                        │  React + Vite   │
                        └────────┬────────┘
                                 │ HTTP
                        ┌────────▼────────┐
                        │   API Gateway   │  :8080
                        │  JWT Filter     │
                        └────┬───┬───┬────┘
                             │   │   │
           ┌─────────────────┘   │   └──────────────────┐
           │                     │                       │
  ┌────────▼──────┐   ┌──────────▼──────┐   ┌──────────▼──────┐
  │ auth-service  │   │ catalog-service  │   │  order-service  │
  │    :8081      │   │     :8082        │   │     :8083       │
  │  PostgreSQL   │   │   PostgreSQL     │   │   PostgreSQL    │
  └───────────────┘   └─────────────────┘   └────────┬────────┘
                                                      │
                                         ┌────────────▼────────────┐
                                         │       RabbitMQ          │
                                         │  order.exchange         │
                                         │  payment.exchange       │
                                         │  auth.exchange          │
                                         └──┬───────────┬──────────┘
                                            │           │
                               ┌────────────▼──┐   ┌───▼──────────────────┐
                               │payment-service│   │ notification-service  │
                               │    :8084      │   │       :8085           │
                               │  PostgreSQL   │   │   (sem banco)         │
                               └───────────────┘   └──────────────────────┘
```

Todos os serviços se comunicam exclusivamente através do API Gateway. A autenticação é validada no gateway, que injeta os headers `X-Auth-Username` e `X-Auth-Role` nas requisições para os serviços downstream — nenhum serviço expõe endpoint público diretamente.

---

## Tecnologias

### Backend
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21+ | Linguagem principal |
| Spring Boot | 4.0.3 | Framework base |
| Spring Security | 6.4 | Autenticação e autorização |
| Spring Cloud Gateway | 2025.x | API Gateway reativo |
| Spring Data JPA | 4.x | Persistência |
| Spring AMQP | 4.x | Mensageria RabbitMQ |
| PostgreSQL | 16 | Banco de dados por serviço |
| RabbitMQ | 3 | Message broker |
| JWT (jjwt) | 0.13.x | Tokens de autenticação |
| Docker / Docker Compose | — | Containerização |

### Frontend
| Tecnologia | Versão | Uso |
|---|---|---|
| React | 19 | UI |
| TypeScript | 5.9 | Tipagem |
| Vite | 7 | Build tool |
| React Router | 7 | Roteamento |

---

## Servicos

### API Gateway — `:8080`

Ponto de entrada único da plataforma. Implementa um `GlobalFilter` que:

1. Libera as rotas públicas `/auth/register` e `/auth/login`
2. Exige header `Authorization: Bearer <token>` em todas as demais rotas
3. Valida a assinatura e expiração do JWT
4. Injeta `X-Auth-Username` e `X-Auth-Role` nos headers da requisição encaminhada
5. Retorna `401 Unauthorized` para tokens ausentes ou inválidos

**Tabela de roteamento:**

| Prefixo | Destino |
|---|---|
| `/auth/**` | `auth-service:8081` |
| `/products/**` | `catalog-service:8082` |
| `/orders/**` | `order-service:8083` |
| `/payments/**` | `payment-service:8084` |
| `/notifications/**` | `notification-service:8085` |

---

### Auth Service — `:8081`

Gerencia identidade e emissão de tokens. Banco de dados: `auth_db`.

**Responsabilidades:**
- Registro de novos usuários com validação de email único
- Login com verificação de senha (BCrypt)
- Geração de JWT com claims de `userId`, `email` e `role`
- Listagem paginada de usuários (restrito a `ADMIN`)
- Publicação de `UserRegisteredEvent` no RabbitMQ após cada registro

**Roles disponíveis:** `USER` (padrão), `ADMIN`

> **⚠️ Para promover um usuário a ADMIN:** o endpoint de registro sempre cria usuários com role `USER`. Para elevar um usuário existente, execute os comandos abaixo diretamente no banco do auth-service:
>
> ```bash
> # Acessar o PostgreSQL do auth-service via Docker
> docker exec -it microservices-postgres-auth-1 psql -U postgres -d auth_db
> ```
>
> ```sql
> -- Dentro do psql, atualizar a role pelo e-mail do usuário
> UPDATE users SET role = 'ADMIN' WHERE email = 'seu@email.com';
>
> -- Confirmar a alteração
> SELECT id, name, email, role FROM users WHERE email = 'seu@email.com';
> ```
>
> Após a atualização o usuário precisa fazer login novamente para receber um JWT com a nova role.

---

### Catalog Service — `:8082`

Gerencia o catálogo de produtos e o controle de estoque. Banco de dados: `catalog_db`.

**Responsabilidades:**
- CRUD completo de produtos (criação e edição restritos a `ADMIN`)
- Consultas por categoria e disponibilidade (estoque > 0)
- Decremento de estoque ao confirmar pedido (consumindo `OrderConfirmedEvent`)
- Saga de compensação: reverte decrementos parciais em caso de falha e publica `OrderCompensatedEvent`
- Publicação de `ProductUpdatedEvent` após atualizações

---

### Order Service — `:8083`

Gerencia o ciclo de vida dos pedidos. Banco de dados: `order_db`.

**Responsabilidades:**
- Criação de pedido com validação de estoque via chamada síncrona ao catalog-service
- Cálculo automático do total do pedido
- Publicação de `OrderCreatedEvent` após criação
- Confirmação de pedido (`PENDING → CONFIRMED`) ao receber `PaymentProcessedEvent`
- Cancelamento de pedido (`PENDING → CANCELLED`) ao receber `PaymentFailedEvent` ou solicitação do usuário

**Status possíveis:** `PENDING` → `CONFIRMED` | `CANCELLED`

---

### Payment Service — `:8084`

Processa pagamentos de forma assíncrona. Banco de dados: `payment_db`.

**Responsabilidades:**
- Consumo de `OrderCreatedEvent` para iniciar o processamento
- Simulação de processamento com taxa de sucesso de 80%
- Publicação de `PaymentProcessedEvent` (sucesso) ou `PaymentFailedEvent` (falha)
- Idempotência: ignora eventos duplicados para o mesmo `orderId`
- Consulta de pagamentos por usuário e por pedido

**Status possíveis:** `PENDING`, `PROCESSED`, `FAILED`

---

### Notification Service — `:8085`

Serviço leve de notificações, sem banco de dados.

**Responsabilidades:**
- Consumo de `OrderConfirmedEvent`, `OrderCancelledEvent` e `PaymentFailedEvent`
- Despacho de notificações ao usuário (atualmente via log estruturado; extensível para email/SMS/push)

---

## Fluxo de Negocio

### Criação e processamento de um pedido

```
Usuário                Order Service           Payment Service        Catalog Service
   │                        │                        │                      │
   │── POST /orders ────────▶│                        │                      │
   │                        │── GET /products/{id} ──────────────────────▶  │
   │                        │◀─── stock info ────────────────────────────── │
   │                        │                        │                      │
   │                        │── salva PENDING ────── │                      │
   │                        │── publica OrderCreatedEvent ──────────────▶   │
   │◀─── 201 Created ───────│                        │                      │
   │                        │                        │── processa pagamento │
   │                        │                        │                      │
   │                        │ ◀── PaymentProcessedEvent ──────────────────  │
   │                        │── atualiza CONFIRMED ──│                      │
   │                        │── publica OrderConfirmedEvent ─────────────▶  │
   │                        │                        │                      │
   │                        │                        │   ◀─ OrderConfirmed  │
   │                        │                        │      decrementa      │
   │                        │                        │      estoque         │
```

### Saga de compensação (falha no estoque)

Se o catalog-service falhar ao decrementar o estoque de um item após já ter processado itens anteriores, ele:

1. Reverte (`incrementStock`) todos os itens já decrementados na mesma transação
2. Publica `OrderCompensatedEvent`
3. Lança exceção para que o RabbitMQ possa reprocessar ou encaminhar para dead-letter

---

## Eventos RabbitMQ

| Exchange | Routing Key | Evento | Publicador | Consumidor(es) |
|---|---|---|---|---|
| `auth.exchange` | `user.registered` | `UserRegisteredEvent` | auth-service | — |
| `order.exchange` | `order.created` | `OrderCreatedEvent` | order-service | payment-service |
| `order.exchange` | `order.confirmed` | `OrderConfirmedEvent` | order-service | catalog-service, notification-service |
| `order.exchange` | `order.cancelled` | `OrderCancelledEvent` | order-service | notification-service |
| `payment.exchange` | `payment.processed` | `PaymentProcessedEvent` | payment-service | order-service |
| `payment.exchange` | `payment.failed` | `PaymentFailedEvent` | payment-service | order-service, notification-service |

Todas as filas são duráveis (`durable=true`). A serialização de mensagens é feita em JSON via `JacksonJsonMessageConverter`.

---

## API Reference

> Todas as rotas abaixo são acessadas via API Gateway em `http://localhost:8080`.
> Rotas marcadas com 🔒 exigem header `Authorization: Bearer <token>`.
> Rotas marcadas com 👑 exigem role `ADMIN`.

### Autenticação

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/register` | Cadastra novo usuário |
| `POST` | `/auth/login` | Autentica e retorna JWT |
| `GET` | `/auth/users` 🔒👑 | Lista todos os usuários (paginado) |

**POST /auth/register**
```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123"
}
```

**POST /auth/login**
```json
{
  "email": "joao@email.com",
  "password": "senha123"
}
```

**Resposta (register / login):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "joao@email.com",
  "name": "João Silva",
  "role": "USER"
}
```

---

### Produtos

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/products` 🔒 | Lista todos os produtos (paginado) |
| `GET` | `/products/available` 🔒 | Lista produtos com estoque > 0 |
| `GET` | `/products/category/{category}` 🔒 | Filtra por categoria |
| `GET` | `/products/{id}` 🔒 | Busca produto por ID |
| `POST` | `/products` 🔒👑 | Cria novo produto |
| `PUT` | `/products/{id}` 🔒👑 | Atualiza produto |
| `DELETE` | `/products/{id}` 🔒👑 | Remove produto |

**POST / PUT /products — body:**
```json
{
  "name": "Teclado Mecânico",
  "description": "Switch Cherry MX Red, RGB",
  "price": 459.90,
  "stockQuantity": 50,
  "category": "Periféricos",
  "imageUrl": "https://exemplo.com/teclado.jpg"
}
```

---

### Pedidos

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/orders` 🔒 | Lista pedidos do usuário autenticado |
| `GET` | `/orders/{id}` 🔒 | Busca pedido por ID |
| `POST` | `/orders` 🔒 | Cria novo pedido |
| `PATCH` | `/orders/{id}/cancel` 🔒 | Cancela pedido (somente `PENDING`) |
| `GET` | `/orders/all` 🔒👑 | Lista todos os pedidos |

**POST /orders — body:**
```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "unitPrice": 459.90
    }
  ]
}
```

**Resposta:**
```json
{
  "id": 42,
  "userId": 7,
  "userEmail": "joao@email.com",
  "status": "PENDING",
  "totalAmount": 919.80,
  "items": [
    { "productId": 1, "quantity": 2, "unitPrice": 459.90 }
  ],
  "createdAt": "2025-03-15T10:00:00",
  "updatedAt": "2025-03-15T10:00:00"
}
```

---

### Pagamentos

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/payments/user` 🔒 | Pagamentos do usuário autenticado |
| `GET` | `/payments/user/{userId}` 🔒👑 | Pagamentos de um usuário específico |
| `GET` | `/payments/order/{orderId}` 🔒 | Pagamento de um pedido específico |

---

## Testes

A suíte de testes cobre todos os serviços com testes unitários. Nenhum teste requer infraestrutura externa (banco de dados, RabbitMQ, etc.) — tudo é mockado ou simulado em memória.

### Executando os testes

```bash
# Todos os testes de um serviço
cd auth-service && mvn test

# Classe específica
mvn test -Dtest=AuthServiceTest

# Todos os serviços de uma vez (da raiz do projeto, se houver parent pom)
mvn test --projects auth-service,catalog-service,order-service,payment-service,notification-service,api-gateway
```

---

### Cobertura por serviço

#### API Gateway

| Classe | Tipo | Cenários cobertos |
|---|---|---|
| `JwtUtilTest` | Unitário | `extractClaims` (token válido, expirado, assinatura inválida, malformado); `isValid` (true/false para cada caso); `extractUsername`; `extractRole` (com e sem claim) |
| `JwtAuthenticationFilterTest` | Unitário | Paths públicos passam sem token; 401 para header ausente e formato incorreto; 401 para token inválido e expirado; propagação correta de `X-Auth-Username` e `X-Auth-Role`; `getOrder() == -1` |

#### Auth Service

| Classe | Tipo | Cenários cobertos |
|---|---|---|
| `JwtServiceTest` | Unitário | Geração de token com claims corretos; extração de username e role; validação (válido, expirado, assinatura inválida) |
| `AuthServiceTest` | Unitário | Registro com sucesso e publicação de evento; e-mail duplicado lança exceção; login correto retorna token; senha incorreta lança exceção; busca de usuário existente e inexistente |
| `AuthControllerTest` | `@WebMvcTest` | `POST /auth/register` — 200, 400 body inválido, 409 e-mail duplicado; `POST /auth/login` — 200, 401 credenciais inválidas; `GET /auth/users` — 200 para ADMIN, 403 para USER, 401 sem auth |

#### Catalog Service

| Classe | Tipo | Cenários cobertos |
|---|---|---|
| `ProductServiceTest` | Unitário | `findAll`, `findAvailable`, `findByCategory` (paginados); `findById` — encontrado e `ProductNotFoundException`; `create` — sucesso e publicação de evento; `update` — sucesso, produto não encontrado; `delete` — sucesso, produto não encontrado; `decrementStock` — sucesso, estoque insuficiente, produto não encontrado |
| `ProductControllerTest` | `@WebMvcTest` | `GET /products` — 200 com página, vazia, 401; `GET /products/available` — 200, vazia; `GET /products/category/{category}` — 200, vazia, 401; `GET /products/{id}` — 200, 404, 401; `POST /products` — 201 (ADMIN), 400 inválido, 403 (USER), 401; `PUT /products/{id}` — 200, 404, 403, 401; `DELETE /products/{id}` — 204, 404, 403, 401 |

#### Order Service

| Classe | Tipo | Cenários cobertos |
|---|---|---|
| `OrderServiceTest` | Unitário | `findByUserId` — paginado, vazio; `findById` — encontrado, `OrderNotFoundException`; `findAll` — paginado; `create` — sucesso + evento + cálculo de total, produto não encontrado, estoque insuficiente, sem evento quando save falha; `confirmOrder` — sucesso + evento, não encontrado, status inválido (CONFIRMED, CANCELLED); `cancelOrder` — sucesso + evento com reason, não encontrado, status inválido |
| `OrderControllerTest` | `@WebMvcTest` | `GET /orders` — 200 com página, vazia, 401; `GET /orders/{id}` — 200, 404, 401; `POST /orders` — 201, 400 body vazio, 400 items vazio, 400 sem productId, 400 quantity zero, 401; `PATCH /orders/{id}/cancel` — 200, 404, 401; `GET /orders/all` — 200 (ADMIN), 403 (USER), 401 |

> **Nota:** o `GlobalExceptionHandler` do order-service foi corrigido para tratar `AccessDeniedException` e `AuthorizationDeniedException` com status 403 em vez de 500. O mesmo ajuste foi aplicado ao payment-service.

#### Payment Service

| Classe | Tipo | Cenários cobertos |
|---|---|---|
| `PaymentServiceTest` | Unitário | `processPayment` — criação do pagamento, publicação de exatamente um evento por chamada, idempotência (pedido duplicado ignorado), dados corretos no `PaymentProcessedEvent`, dados corretos no `PaymentFailedEvent`; `getPaymentsByUser` — paginado, vazio; `getPaymentByOrder` — encontrado (PROCESSED), encontrado (FAILED com reason), `PaymentNotFoundException` |
| `PaymentControllerTest` | `@WebMvcTest` | `GET /payments/user` — 200 com página, vazia, 401; `GET /payments/user/{userId}` — 200 (ADMIN), 403 (USER), 401; `GET /payments/order/{orderId}` — 200 PROCESSED, 200 FAILED com failureReason, 404, 401 |

#### Notification Service

| Classe | Tipo | Cenários cobertos |
|---|---|---|
| `NotificationServiceTest` | Unitário | `notifyOrderConfirmed` — log INFO com orderId, userId e totalAmount; `notifyOrderCancelled` — log INFO com orderId, userId e reason; `notifyPaymentFailed` — log INFO com orderId, userId e reason; exatamente um log por chamada em cada método |
| `NotificationListenerTest` | Unitário | `onOrderConfirmed` delega ao service com o evento correto; `onOrderCancelled` delega ao service com o evento correto; `onPaymentFailed` delega ao service com o evento correto; nenhuma interação adicional com o service |
| `JwtUtilTest` | Unitário | `extractClaims`, `extractUsername`, `isTokenValid` — token válido, expirado, assinatura inválida |
| `JwtAuthFilterTest` | Unitário | Sem header passa adiante; header inválido retorna 401; token inválido retorna 401; token válido popula `SecurityContext` com username e authorities |

---

### Padrões e decisões de teste

**`@WebMvcTest` + `TestSecurityConfig`**
Cada serviço com controller possui uma `TestSecurityConfig` em `src/test/java/.../config/` que substitui o `JwtAuthFilter` de produção por um filtro no-op. Isso evita a necessidade de configurar segredos JWT nos testes e permite usar `@WithMockUser` para simular autenticação.

**Serialização de paginação**
O formato do JSON paginado varia conforme a presença de `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` na aplicação:
- **Com** essa anotação (catalog-service): `$.page.totalElements`
- **Sem** essa anotação (order-service, payment-service): `$.totalElements` na raiz

**`BigDecimal` e escala**
O Jackson serializa `BigDecimal` preservando a escala original. `new BigDecimal("299.90")` é serializado como `"299.90"`, não `"299.9"`. Os testes usam strings com a escala exata para evitar falsos negativos.

**`processPayment` não determinístico**
O `PaymentService` usa `Random` internamente (80% sucesso). Os testes de dados de evento usam loop de 100 tentativas para garantir que cada caminho (sucesso e falha) seja exercido ao menos uma vez — a probabilidade de 100 execuções consecutivas do mesmo tipo é desprezível.

**`NotificationService` via `ListAppender`**
Como o serviço não tem dependências externas (apenas escreve logs), os testes capturam os eventos de log usando `ListAppender<ILoggingEvent>` do Logback, sem necessidade de mocks.

**Gateway reativo (`WebFlux`)**
O api-gateway usa Spring Cloud Gateway (reativo). Os testes do filtro usam `MockServerHttpRequest` + `MockServerWebExchange` + `StepVerifier` do Reactor em vez de `MockMvc`. A captura do exchange mutado pelo filtro usa `ServerWebExchange` (interface) em vez da classe concreta de mock, pois `exchange.mutate().build()` retorna um `MutativeDecorator`, não o objeto original.

---

## Configuracao e Execucao

### Pré-requisitos

- Docker 24+
- Docker Compose 2.20+
- (Opcional) Node.js 20+ para desenvolvimento do frontend

### Subindo o ambiente completo

```bash
# Clone o repositório
git clone <url-do-repositorio>
cd microservices

# Sobe todos os serviços
docker compose up --build

# Em background
docker compose up --build -d
```

O Docker Compose sobe automaticamente, na ordem correta (via `depends_on` + healthchecks):

1. PostgreSQL (4 instâncias) + RabbitMQ
2. auth-service, catalog-service, order-service, payment-service, notification-service
3. api-gateway

### Verificando os serviços

```bash
# Status dos containers
docker compose ps

# Logs de um serviço específico
docker compose logs -f order-service

# Parar tudo
docker compose down

# Parar e remover volumes
docker compose down -v
```

### Acessos

| Serviço | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Frontend | http://localhost:5173 (dev) |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| auth-service (direto) | http://localhost:8081 |
| catalog-service (direto) | http://localhost:8082 |
| order-service (direto) | http://localhost:8083 |
| payment-service (direto) | http://localhost:8084 |
| notification-service (direto) | http://localhost:8085 |

### Executando o frontend em modo desenvolvimento

```bash
cd frontend
npm install
npm run dev
```

O frontend conecta ao gateway em `http://localhost:8080` via proxy configurado no `vite.config.ts`.

### Health checks

Todos os serviços expõem o Spring Boot Actuator:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
# etc.
```

---

## Variaveis de Ambiente

As configurações sensíveis são centralizadas nos arquivos `application.properties` de cada serviço. Para ambientes de produção, substitua os valores abaixo via variáveis de ambiente ou secrets manager.

| Variável | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | `3f8a2b1c...` | Chave HMAC para assinatura JWT (compartilhada entre todos os serviços) |
| `JWT_EXPIRATION` | `86400000` | Expiração do token em ms (24h) |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuário do PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Senha do PostgreSQL |
| `SPRING_RABBITMQ_USERNAME` | `guest` | Usuário do RabbitMQ |
| `SPRING_RABBITMQ_PASSWORD` | `guest` | Senha do RabbitMQ |

### Perfis disponíveis

| Perfil | Arquivo | Uso |
|---|---|---|
| (padrão) | `application.properties` | Desenvolvimento local com Docker |
| `docker` | `application-docker.properties` | Containers com hostnames do Compose |
| `test` | `application-test.properties` | Testes com H2 in-memory (auth-service) |

Para ativar um perfil: `SPRING_PROFILES_ACTIVE=docker`

---

## Estrutura do Projeto

```
microservices/
├── docker-compose.yml
├── api-gateway/
│   ├── Dockerfile
│   ├── pom.xml
│   └── main/
│       ├── java/com/api/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/JwtAuthenticationFilter.java
│       │   └── util/JwtUtil.java
│       └── resources/
│           ├── application.properties
│           └── application-docker.properties
├── auth-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── main/java/com/auth/service/
│       ├── config/         # SecurityConfig, JwtAuthFilter, RabbitMQConfig
│       ├── controller/     # AuthController
│       ├── domain/         # User, Role
│       ├── dto/            # LoginRequest, RegisterRequest, AuthResponse, UserResponse
│       │   └── events/     # UserRegisteredEvent
│       ├── exception/      # GlobalExceptionHandler, exceções de domínio
│       ├── messaging/      # AuthEventPublisher
│       ├── repository/     # UserRepository
│       └── service/        # AuthService, JwtService
├── catalog-service/        # estrutura análoga ao auth-service
├── order-service/          # estrutura análoga ao auth-service
├── payment-service/        # estrutura análoga ao auth-service
├── notification-service/   # estrutura análoga ao auth-service
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── components/     # Btn, Input, Modal, Card, Spinner, Toast, etc.
        ├── contexts/       # AuthContext
        ├── pages/          # AuthPage, StorePage, MyOrdersPage, ProductsPage, etc.
        └── services/       # api.ts
```

---

## Frontend

A interface é uma SPA (Single Page Application) que se comunica exclusivamente com o API Gateway.

### Páginas por role

| Página | Role | Descrição |
|---|---|---|
| `/` (AuthPage) | público | Login e cadastro |
| `/store` | USER | Vitrine de produtos com carrinho |
| `/my-orders` | USER | Pedidos e histórico do usuário |
| `/payments` | USER | Histórico de pagamentos |
| `/dashboard` | ADMIN | Visão geral do sistema |
| `/products` | ADMIN | CRUD de produtos |
| `/orders` | ADMIN | Todos os pedidos com ações |

### Fluxo de compra no frontend

1. Usuário navega pela loja (`/store`) e adiciona produtos ao carrinho
2. Ao finalizar, o `CartModal` chama `POST /orders`
3. O frontend inicia polling em `GET /orders/{id}` aguardando a transição de `PENDING` para `CONFIRMED` ou `CANCELLED`
4. O resultado é exibido via `Toast` de sucesso ou falha

### Autenticação

O `AuthContext` armazena o token em memória (não em `localStorage`) e o injeta automaticamente em todas as requisições via `apiFetch`. Em caso de `401`, o handler global desloga o usuário e redireciona para a tela de login.