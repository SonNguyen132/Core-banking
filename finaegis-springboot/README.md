# FinAegis Core Banking — Bản Axon (Event Sourcing / CQRS / Saga bằng Axon Framework)

Port sang **Spring Boot 3 + Java 17** của nền tảng FinAegis Core Banking, sử dụng **Axon Framework 4.11** để triển khai Event Sourcing, CQRS và Saga. Đây là bản "đầy đủ framework" — Axon lo phần lớn hạ tầng phức tạp (event store, command/query bus, saga lifecycle, optimistic concurrency).

> 🔀 **So sánh**: bản anh em viết tay (không Axon) nằm ở `../finaegis-springboot-pure`.

---

## 1. Tổng quan kiến trúc

```
                    ┌─────────────────────────────────────────────┐
                    │                  REST Controllers            │
                    │  (Auth, Account, Transfer, Asset, Exchange, │
                    │   Loan, Stablecoin, Governance)              │
                    └───────────────┬─────────────────────────────┘
                                    │
                    ┌───────────────▼─────────────────────────────┐
                    │            Axon Framework                    │
                    │                                              │
                    │   CommandGateway ──► Command Handlers        │
                    │   QueryGateway  ──► Query Handlers           │
                    │                                              │
                    │   Aggregates  (Account, Transfer, Order)     │
                    │   Sagas       (TransferSaga)                 │
                    │   Event Store (auto append + replay)         │
                    └───────────────┬─────────────────────────────┘
                                    │
                    ┌───────────────▼─────────────────────────────┐
                    │        Read Models (Projections)             │
                    │  AccountView, TransferView, AccountBalance   │
                    └─────────────────────────────────────────────┘
```

**Các thành phần Axon được dùng:**
- `@Aggregate` + `@CommandHandler` + `@EventSourcingHandler` — Aggregate event-sourced
- `CommandGateway` — gửi command bất đồng bộ / đồng bộ (`sendAndWait`)
- `QueryGateway` — truy vấn read-side
- `@Saga`, `@StartSaga`, `@SagaEventHandler`, `@EndSaga`, `SagaLifecycle` — distributed transaction
- `AggregateLifecycle.apply()` — phát sinh sự kiện, Axon tự lưu vào event store & replay để dựng lại trạng thái

---

## 2. Cấu trúc thư mục

```
finaegis-springboot/
├── pom.xml                          # Spring Boot 3.2 + Axon 4.11.3
├── README.md
└── src/
    ├── main/
    │   ├── java/com/finaegis/
    │   │   ├── FinAegisApplication.java
    │   │   ├── config/              # SecurityConfig, GlobalExceptionHandler
    │   │   ├── security/            # User, Role, Permission, JWT...
    │   │   ├── presentation/
    │   │   │   ├── dto/             # RegisterRequest, AuthenticationRequest...
    │   │   │   └── rest/            # 8 REST controllers
    │   │   └── domain/
    │   │       ├── account/         # Aggregate + commands + events + projection
    │   │       ├── payment/         # Transfer aggregate + saga
    │   │       ├── asset/           # Multi-asset balance + exchange rate
    │   │       ├── exchange/        # Order aggregate + order book + matching engine
    │   │       ├── lending/         # P2P loans
    │   │       ├── stablecoin/      # Reserve management
    │   │       └── governance/      # Polls & voting
    │   └── resources/
    │       ├── application.yml      # datasource + Axon Server config + JWT
    │       └── db/migration/V1__init_schema.sql
    └── test/                        # Account, MatchingEngine, Loan, Stablecoin tests
```

### Domain: chi tiết từng bounded context

| Domain | Aggregate | Commands | Events | Projection / Read model |
|--------|-----------|----------|--------|--------------------------|
| **account** | `AccountAggregate` | Create, Deposit, Withdraw, Freeze, Unfreeze, Close | Created, Deposited, Withdrawn, Frozen, Unfrozen, Closed | `AccountView` (bảng `account_view`) |
| **payment** | `TransferAggregate` | Initiate, Complete | Initiated, Debited, Credited, Completed, Failed, RolledBack | `TransferView` |
| **exchange** | `OrderAggregate` | Place, Cancel | Placed, Matched, Filled, Cancelled | `OrderBook` (in-memory) |
| **asset** | — (service) | — | — | `Asset`, `ExchangeRate`, `AccountBalance` |
| **lending** | — (service) | — | — | `Loan` |
| **stablecoin** | — (service) | — | — | `StablecoinSupply` |
| **governance** | — (service) | — | — | `Poll`, `Vote` |

---

## 3. Flow nghiệp vụ chi tiết

### 3.1 Flow tài khoản (Account)

```
POST /api/v1/accounts
   │  body: { name, userId, assetCode }
   ▼
Controller gọi commandGateway.sendAndWait(CreateAccountCommand)
   ▼
Axon gửi tới AccountAggregate.create()      (@CommandHandler)
   ├── kiểm tra name/assetCode hợp lệ
   └── AggregateLifecycle.apply(AccountCreatedEvent)
          ▼
Axon: lưu event vào event store + gọi @EventSourcingHandler (cập nhật state)
   ▼
Projection (AccountProjectionHandler) lắng nghe event
   ▼
Tạo dòng AccountView trong account_view (read model)
```

**Các thao tác tiếp theo** cùng luồng tương tự:
- **Deposit** `POST /api/v1/accounts/{id}/deposit` → `DepositMoneyCommand` → kiểm tra `ensureActive()` (không frozen/closed) + amount > 0 → `MoneyDepositedEvent` → cộng balance (aggregate + projection).
- **Withdraw** → kiểm tra `ensureActive()` + **số dư đủ** (`InsufficientFundsException` nếu thiếu) → `MoneyWithdrawnEvent` → trừ balance.
- **Freeze / Unfreeze / Close** → chuyển trạng thái; Close chỉ được khi số dư = 0.

> **Lưu ý**: Aggregates của Axon deprecated `@AggregateIdentifier` trong 4.x mới, nhưng code dùng cú pháp tương thích 4.x (đã note trong pom).

### 3.2 Flow chuyển tiền — Saga (quan trọng nhất)

**Axon Saga** là cách triển khai **distributed transaction** xuyên qua nhiều aggregate, có khả năng bù trừ (compensation).

```
POST /api/v1/transfers
   │  body: { fromAccountId, toAccountId, amount, currency, ... }
   ▼
commandGateway.sendAndWait(InitiateTransferCommand)
   ▼
TransferAggregate @CommandHandler → apply(TransferInitiatedEvent)
   ▼
Axon khởi tạo TransferSaga (@StartSaga)
   │
   ├─ Bước 1: commandGateway.send(WithdrawMoneyCommand)  → ghi nợ nguồn
   │           └── chờ MoneyWithdrawnEvent (onDebitConfirmed)
   │
   ├─ Bước 2: commandGateway.send(DepositMoneyCommand)   → ghi có đích
   │           └── chờ MoneyDepositedEvent (onCreditConfirmed)
   │
   ├─ Bước 3: commandGateway.send(CompleteTransferCommand)
   │           → TransferAggregate apply(TransferCompletedEvent) → saga kết thúc
   │
   └─ Nếu lỗi: failTransfer()
       └── nếu đã ghi nợ → COMPENSATION: DepositMoneyCommand ngược về nguồn
```

Saga **duy trì trạng thái** qua các field (`fromAccountId`, `toAccountId`, `amount`, `debitConfirmed`) và **association** bằng `accountId`/`transferId` để Axon định tuyến đúng event về đúng instance Saga.

### 3.3 Flow sàn giao dịch (Exchange / Matching Engine)

```
POST /api/v1/exchange/orders
   │  body: { accountId, symbol, side, type, quantity, price }
   ▼
commandGateway.sendAndWait(PlaceOrderCommand)
   ▼
OrderAggregate → OrderPlacedEvent
   ▼
Controller đồng thời đưa OrderBookEntry vào MatchingEngine (@Service)
   ├── addBuyOrder / addSellOrder theo side
   └── match() theo price-time priority:
         BUY giảm dần giá, SELL tăng dần giá
         Nếu bestBuy.price >= bestSell.price → khớp lệnh
```

### 3.4 Flow stablecoin (mint/burn)

```
POST /api/v1/stablecoins/mint   { symbol, amount, targetAccount }
   ▼
StablecoinService.mint()
   ├── reserve_amount += amount
   └── total_supply += amount        (mint 1:1, đảm bảo luôn được đảm bảo)

POST /api/v1/stablecoins/burn   { symbol, amount, targetAccount }
   ▼
StablecoinService.burn()
   ├── yêu cầu total_supply đủ
   ├── total_supply -= amount
   └── reserve_amount -= amount

GET /api/v1/stablecoins/{symbol}/collateral-ratio
   └── ratio = reserve_amount / total_supply * 100 → so sánh minCollateralRatio
```

### 3.5 Flow vay (Lending) & bỏ phiếu (Governance)

- **Loan**: `apply` → tạo `Loan` trạng thái `PENDING` + tính `monthlyPayment` (công thức amortization); `approve` → đổi sang `ACTIVE` (chỉ khi đang PENDING).
- **Governance**: `createPoll` → `ACTIVE`; `vote` → chống trùng (`existsByPollIdAndUserId`); `results` → group theo `optionKey`.

---

## 4. Bảo mật (JWT + RBAC)

```
POST /api/v1/auth/register  → tạo User + trả accessToken/refreshToken
POST /api/v1/auth/login     → xác thực qua AuthenticationManager → trả token
GET  /api/v1/auth/me        → lấy thông tin user hiện tại
```

- Mật khẩu **bcrypt** (`BCryptPasswordEncoder`).
- `JwtService` sign token bằng `hmacShaKeyFor(secret)` (BASE64), TTL cấu hình trong `application.yml`.
- `JwtAuthenticationFilter` chặn request, gắn `UsernamePasswordAuthenticationToken` vào context.
- `SecurityConfig`: stateless session, CORS, permit `/api/v1/auth/**`, yêu cầu authenticated cho phần còn lại.
- Roles/Permissions: quan hệ `users — user_roles — roles — role_permissions — permissions`.

---

## 5. API Reference

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/auth/register` | Đăng ký |
| POST | `/api/v1/auth/login` | Đăng nhập |
| GET | `/api/v1/auth/me` | Hồ sơ hiện tại |
| GET | `/api/v1/accounts?userId=` | Danh sách account theo user |
| GET | `/api/v1/accounts/{id}` | Chi tiết account |
| POST | `/api/v1/accounts` | Tạo account |
| POST | `/api/v1/accounts/{id}/deposit` | Nạp tiền |
| POST | `/api/v1/accounts/{id}/withdraw` | Rút tiền |
| POST | `/api/v1/accounts/{id}/freeze` | Đóng băng |
| POST | `/api/v1/accounts/{id}/unfreeze` | Mở đóng băng |
| POST | `/api/v1/transfers` | Khởi tạo chuyển tiền (Saga) |
| GET | `/api/v1/transfers/{id}` | Trạng thái chuyển tiền |
| GET | `/api/v1/assets` | Danh sách tài sản |
| GET | `/api/v1/assets/rates` | Danh sách tỉ giá |
| GET | `/api/v1/assets/rates/{from}/{to}` | Tỉ giá 1 cặp |
| POST | `/api/v1/assets/rates/convert` | Convert FX |
| POST | `/api/v1/exchange/orders` | Đặt lệnh |
| DELETE | `/api/v1/exchange/orders/{id}` | Hủy lệnh |
| POST | `/api/v1/loans/apply` | Vay |
| POST | `/api/v1/loans/{id}/approve` | Duyệt vay |
| GET | `/api/v1/loans/borrower/{id}` | Danh sách vay theo borrower |
| POST | `/api/v1/stablecoins/mint` | Mint stablecoin |
| POST | `/api/v1/stablecoins/burn` | Burn stablecoin |
| GET | `/api/v1/stablecoins/{sym}/supply` | Cung cấp hiện tại |
| GET | `/api/v1/stablecoins/{sym}/collateral-ratio` | Tỉ lệ tài sản đảm bảo |
| GET | `/api/v1/governance/polls` | Poll đang active |
| POST | `/api/v1/governance/polls` | Tạo poll |
| POST | `/api/v1/governance/polls/{id}/vote` | Bỏ phiếu |
| GET | `/api/v1/governance/polls/{id}/results` | Kết quả |

---

## 6. Chạy & cấu hình

```bash
# 1) DB
mysql -u root -p -e "CREATE DATABASE finaegis CHARACTER SET utf8mb4;"

# 2) Cấu hình trong src/main/resources/application.yml
spring.datasource.url / username / password
DB_PASSWORD=pwd export   # hoặc set env

# 3) Build & test
./mvnw clean test          # (hoặc mvn)

# 4) Chạy
mvn spring-boot:run
```

**Phụ thuộc hạ tầng:**
- MySQL
- Redis (cache, mặc định localhost:6379)
- Axon Server (mặc định localhost:8124) — nếu không có Server, Axon có chế độ "embedded" fallback cho event store đơn giản

> ⚠️ **Lưu ý xây dựng**: Do egress filter mạng, các artifact `io/axonframework:...` có thể trả về 404 khi dùng `mvn` trong một số môi trường. Build này cần truy cập Maven Central đầy đủ; trên máy có mạng bình thường `mvn clean test` sẽ chạy.

---

## 7. Cấu hình Axon (application.yml)

```yaml
axon:
  axonserver:
    servers: ${AXON_SERVER:localhost:8124}
  eventstore:
    jdbc:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/finaegis_events?...
```

Axon quản lý **event store riêng** (ở đây cấu hình JDBC). Read models (projection) lưu ở schema chính `finaegis`.

---

## 8. Testing

```bash
mvn test
```

| Test | Mục đích |
|------|----------|
| `AccountAggregateTest` | Dùng `AggregateTestFixture` của Axon: given/when/expect events hoặc exception |
| `MatchingEngineTest` | Khớp lệnh mua/bán, không khớp khi giá không cắt |
| `LoanServiceTest` | Tính monthly payment, apply vay |
| `StablecoinServiceTest` | Mint/burn, tính collateral ratio |
