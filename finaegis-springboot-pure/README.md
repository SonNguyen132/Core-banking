# FinAegis Core Banking — Bản thuần (Event Sourcing viết tay, không Axon)

Port sang **Spring Boot 3.2 + Java 17** của nền tảng FinAegis Core Banking, dùng **Event Sourcing + CQRS + Saga tự viết** trên nền Spring/JPA thay vì Axon Framework. Mục đích: **nhìn thấy rõ toàn bộ cơ chế** Event Sourcing hoạt động như thế nào (thay vì bị framework che giấu).

> 🔀 **So sánh**: bản đầy đủ framework dùng Axon nằm ở `../finaegis-springboot`.
> ✅ Bản này **build & test thành công trong môi trường offline/egress filter** (không phụ thuộc artifacts Axon).

---

## 1. Tổng quan kiến trúc

```
                    ┌─────────────────────────────────────────────┐
                    │             REST Controllers (8)             │
                    └───────────────┬─────────────────────────────┘
                                    │
        ┌───────────────────────────▼───────────────────────────┐
        │                CQRS (viết tay)                         │
        │                                                        │
        │  WRITE SIDE                READ SIDE                   │
        │  *CommandService           *QueryService / Projection  │
        │      │                              ▲                   │
        │      ▼                              │ (event)           │
        │  AggregateRoot ◄── apply/on ◄───────┘                   │
        │  AggregateRepository             @EventListener         │
        │      │                              │                   │
        │      ▼                              │                   │
        │  EventStore (append + publish) ─────┘                   │
        │      │                                                 │
        │      ▼                                                 │
        │  stored_events (bảng JPA)          read models (JPA)   │
        └────────────────────────────────────────────────────────┘
```

**Vai trò các lớp (ánh xạ với Axon):**

| Lớp tự viết | Tương đương Axon | Vai trò |
|-------------|------------------|---------|
| `AggregateRoot` | `@Aggregate` | Base cho mọi aggregate; track version + uncommitted changes; dispatch event |
| `AggregateRepository` | `RepositoryProvider` | Load (replay) / Save (append + publish) |
| `EventStore` | Axon Event Store | Ghi `stored_events`, kiểm tra optimistic locking |
| `SpringEventPublisher` | `EventGateway` | Đưa event vào Spring event bus |
| `@EventListener` (projection) | `@EventHandler` | Cập nhật read model |
| `TransferSaga` (@Component) | `@Saga` | Điều phối multi-step + bù trừ |

---

## 2. Cấu trúc thư mục

```
finaegis-springboot-pure/
├── pom.xml                              # Spring Boot 3.2 (KHÔNG có Axon)
├── README.md
└── src/
    ├── main/
    │   ├── java/com/finaegis/
    │   │   ├── FinAegisApplication.java
    │   │   ├── config/                  # SecurityConfig, GlobalExceptionHandler
    │   │   ├── security/                # User, Role, Permission, JWT...
    │   │   ├── eventstore/              # ★ Lõi Event Sourcing tự viết
    │   │   │   ├── AggregateRoot.java
    │   │   │   ├── AggregateRepository.java
    │   │   │   ├── EventStore.java
    │   │   │   ├── EventStoreRepository.java
    │   │   │   ├── EventPublisher.java / SpringEventPublisher.java
    │   │   │   ├── StoredEvent.java
    │   │   │   └── ConcurrentModificationException.java
    │   │   ├── presentation/            # dto + rest controllers
    │   │   └── domain/
    │   │       ├── common/DomainEvent.java   # base event (+ metadata aggId/version)
    │   │       ├── account/             # aggregate + events + projection + command/query
    │   │       ├── payment/             # Transfer aggregate + saga + projection
    │   │       ├── asset/               # assets + exchange rate + convert
    │   │       ├── exchange/            # order book + matching engine
    │   │       ├── lending/             # P2P loans
    │   │       ├── stablecoin/          # mint/burn + collateral ratio
    │   │       └── governance/          # polls + votes
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/V1__init_schema.sql   # có bảng stored_events
    └── test/                            # 27 tests (JUnit5 + Mockito)
```

---

## 3. Lõi Event Sourcing tự viết (eventstore/)

### 3.1 `StoredEvent` — bảng `stored_events`

Mỗi sự kiện là một dòng bất biến trong bảng:

| Cột | Ý nghĩa |
|-----|---------|
| `stream_id` | luồng logic (thường = aggregate id) |
| `aggregate_id` | aggregate sở hữu |
| `aggregate_type` | bộ định danh, VD `com.finaegis.domain.account` |
| `event_type` | tên class event, VD `MoneyDepositedEvent` |
| `event_version` | version tăng dần trong stream (dùng optimistic locking) |
| `event_data` | JSON payload |

### 3.2 `AggregateRoot` — base class

- Giữ `aggregateId`, `version`, danh sách `changes` (chưa ghi).
- `apply(DomainEvent)` (protected): gắn metadata, thêm vào `changes`, gọi `when(event)`.
- `when(event)` dùng **reflection** để tìm `on(ConcreteEvent)` và áp dụng lên state.
  > ⚠️ **Gotcha quan trọng**: handlers phải đặt tên **`on(...)`**, không được tên `apply(...)` — vì `apply(ConcreteEvent)` trong Java sẽ **overload-shadow** lên base `apply(DomainEvent)`, khiến event không được ghi nhận. (Đã xử lý trong code.)
- `rebuildFrom(...)` replay `stored_events` để dựng lại state.

### 3.3 `AggregateRepository` — load/save

```
load(streamId, type, factory)
   ├── đọc stored_events theo stream → replay → aggregate có state
   └── nếu trống → trả aggregate mới (factory)

save(aggregate, expectedVersion)
   ├── nếu không có changes → return
   └── với mỗi event: append (kiểm tra version) → publish → clear changes
```

### 3.4 `EventStore` — append + optimisitic locking

```
append(streamId, aggId, type, expectedVersion, event)
   ├── current = maxVersion(stream)
   ├── nếu current != expectedVersion → ConcurrentModificationException
   ├── newVersion = current + 1
   ├── serialize JSON → lưu StoredEvent
   └── đánh dấu recorded
```

`publishAll(...)` đưa các event đã ghi vào `SpringEventPublisher` → các `@EventListener` (projection, saga) xử lý.

---

## 4. Flow nghiệp vụ chi tiết

### 4.1 Flow tài khoản (Account)

```
POST /api/v1/accounts
   ▼
AccountCommandService.create()            (classic CQRS write-side)
   ▼
aggregateRepository.load(accId, Account.TYPE, Account::new)
   ▼
account.create(name, userId, assetCode)   (command method trên aggregate)
   ├── kiểm tra hợp lệ
   └── apply(new AccountCreatedEvent(...))
          ├── thêm vào changes
          └── when → on(AccountCreatedEvent) → set state
   ▼
aggregateRepository.save(account, version)
   ├── EventStore.append → ghi stored_events (version+1)
   └── publishAll → Spring event bus
   ▼
AccountProjection @EventListener(on AccountCreatedEvent)
   └── tạo AccountView vào account_view (read model)
```

**Các thao tác khác** (deposit / withdraw / freeze / unfreeze / close) đi đúng luồng trên:
- **Deposit**: `ensureActive()` + amount > 0 → `MoneyDepositedEvent` → cộng balance (aggregate + projection).
- **Withdraw**: `ensureActive()` + **kiểm tra số dư đủ** (nếu không → `IllegalStateException`) → `MoneyWithdrawnEvent` → trừ balance.
- **Freeze/Unfreeze/Close**: chuyển trạng thái; Close chỉ khi số dư = 0.
- Đọc dữ liệu qua **`AccountQueryService`** (read-side) — tách biệt hẳn với write-side.

### 4.2 Flow chuyển tiền — Saga tự viết (quan trọng)

`TransferSaga` là một `@Component` lắng nghe `TransferInitiatedEvent`, thực hiện **orchestration + compensation** bằng tay:

```
POST /api/v1/transfers
   ▼
TransferApplicationService.initiate()
   ▼
Transfer aggregate → initiate() → apply(TransferInitiatedEvent)
   ▼
aggregateRepository.save → publish
   ▼
★ TransferSaga @EventListener(TransferInitiatedEvent)
   │
   ├─ Bước 1: accountCommandService.withdraw(from, amount)   // ghi nợ nguồn
   ├─ Bước 2: accountCommandService.deposit(to, amount)      // ghi có đích
   ├─ Bước 3: Transfer.complete() → TransferCompletedEvent   // hoàn tất
   │
   └─ catch (lỗi bất kỳ):
       └─ compensate():
            accountCommandService.deposit(from, amount, "COMPENSATE-…")  // hoàn trả nguồn
            Transfer.fail(reason) → TransferFailedEvent                   // đánh dấu FAILED
```

**Điểm bền vững (durability)**: trái ngược saga lưu state trong RAM, saga này **đọc lại trạng thái từ `Transfer` aggregate (event-sourced)** nên chịu được restart. Mỗi bước dùng `reference = "TRANSFER-<step>-<transferId>"` để truy vết/idempotent.

So với **bản Axon**:
- Axon: `@StartSaga`/`@SagaEventHandler` + `SagaLifecycle.end()` — Axon tự quản lý vòng đời saga & định tuyến event.
- Pure: vòng lặp try/catch sync, gọi trực tiếp `AccountCommandService`, tự ghi `TransferFailedEvent`.

### 4.3 Flow sàn giao dịch (Exchange / Matching Engine)

```
POST /api/v1/exchange/orders   { accountId, symbol, side, type, quantity, price }
   ▼
ExchangeService.placeOrder()
   ▼
tạo OrderBookEntry → MatchingEngine
   ├── side BUY → addBuyOrder(symbol, entry)
   ├── side SELL → addSellOrder(symbol, entry)
   └── match(): price-time priority
         bestBuy.price >= bestSell.price → khớp, trừ remaining qty, poll khi đầy
```

> Matching engine là in-memory (stateful) — phù hợp demo; production nên persist các lần khớp bằng event.

### 4.4 Flow stablecoin (mint/burn)

```
mint(symbol, fiat, account):
   reserve_amount += fiat
   total_supply   += fiat        // 1:1

burn(symbol, tokens, account):
   yêu cầu total_supply đủ  (nếu không → IllegalStateException)
   total_supply   -= tokens
   reserve_amount -= tokens

collateral-ratio: reserve / total_supply * 100  vs  minCollateralRatio (100)
```

### 4.5 Flow vay (Lending) & Governance

- **Loan**: `loanService.apply(...)` → `PENDING` + tính `monthlyPayment` (công thức amortization); `approve(id, lender)` → `ACTIVE` (chỉ khi PENDING).
- **Governance**: `governanceService.createPoll(...)` → `ACTIVE`; `vote(...)` chống trùng `existsByPollIdAndUserId`; `results(...)` group theo `optionKey`.

---

## 5. Bảo mật (JWT + RBAC)

Giống hệt bản Axon (không phụ thuộc framework):
- `POST /api/v1/auth/register` / `login` → trả `accessToken` + `refreshToken`.
- Mật khẩu **bcrypt**; `JwtService` dùng HMAC-SHA (base64 secret) sign token.
- `JwtAuthenticationFilter` gắn authentication vào Spring Security context.
- `SecurityConfig`: stateless, CORS, permit `/api/v1/auth/**`, còn lại authenticated.
- RBAC: `users–user_roles–roles–role_permissions–permissions`.

---

## 6. API Reference

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/auth/register` | Đăng ký |
| POST | `/api/v1/auth/login` | Đăng nhập |
| GET | `/api/v1/auth/me` | Hồ sơ hiện tại |
| GET | `/api/v1/accounts?userId=` | Danh sách account |
| GET | `/api/v1/accounts/{id}` | Chi tiết account |
| POST | `/api/v1/accounts` | Tạo account |
| POST | `/api/v1/accounts/{id}/deposit` | Nạp tiền |
| POST | `/api/v1/accounts/{id}/withdraw` | Rút tiền |
| POST | `/api/v1/accounts/{id}/freeze` \| `/unfreeze` \| `/close` | State transitions |
| POST | `/api/v1/transfers` | Chuyển tiền (saga) |
| GET | `/api/v1/transfers/{id}` | Trạng thái transfer |
| GET | `/api/v1/transfers?accountId=` | Lịch sử theo account |
| GET | `/api/v1/assets` | Danh sách tài sản |
| GET | `/api/v1/assets/rates` | Tỉ giá |
| GET | `/api/v1/assets/rates/{from}/{to}` | Tỉ giá 1 cặp |
| POST | `/api/v1/assets/rates/convert` | Convert FX |
| POST | `/api/v1/exchange/orders` | Đặt lệnh |
| DELETE | `/api/v1/exchange/orders/{id}` | Hủy lệnh |
| POST | `/api/v1/loans/apply` | Vay |
| POST | `/api/v1/loans/{id}/approve` | Duyệt vay |
| GET | `/api/v1/loans/borrower/{id}` | Vay theo borrower |
| POST | `/api/v1/stablecoins/mint` | Mint |
| POST | `/api/v1/stablecoins/burn` | Burn |
| GET | `/api/v1/stablecoins/{sym}/supply` | Cung cấp |
| GET | `/api/v1/stablecoins/{sym}/collateral-ratio` | Tỉ lệ đảm bảo |
| GET | `/api/v1/governance/polls` | Poll active |
| GET | `/api/v1/governance/polls/all` | Tất cả polls |
| POST | `/api/v1/governance/polls` | Tạo poll |
| POST | `/api/v1/governance/polls/{id}/vote` | Bỏ phiếu |
| GET | `/api/v1/governance/polls/{id}/results` | Kết quả |

---

## 7. Chạy & cấu hình

```bash
# 1) DB
mysql -u root -p -e "CREATE DATABASE finaegis_pure CHARACTER SET utf8mb4;"

# 2) Cấu hình src/main/resources/application.yml
spring.datasource.url / username / password

# 3) Build & test (KHÔNG phụ thuộc Axon — chạy cả môi trường offline)
mvn clean test

# 4) Chạy
mvn spring-boot:run
```

Khác bản Axon: **không cần** Axon Server / RabbitMQ / Kafka. Chỉ cần MySQL.

---

## 8. Testing

```bash
mvn clean test        # ✅ 27 tests, 0 lỗi
```

| Test | Mô tả |
|------|-------|
| `AccountAggregateTest` (12) | create/deposit/withdraw/overdraft/frozen/close + serialize event |
| `MatchingEngineTest` (4) | khớp lệnh, partial fill, không khớp, price-time priority |
| `LoanServiceTest` (5) | monthly payment, apply, approve + guard |
| `StablecoinServiceTest` (6) | mint/burn, collateral ratio, burn-quá-cung |

> Dùng **JUnit5 + Mockito** thuần (không Axon Test Fixture).

---

## 9. Khác biệt chính với bản Axon

| Tiêu chí | Axon | Pure (này) |
|----------|------|------------|
| Event dispatch | Annotation `@EventSourcingHandler` | Reflection `when()` → `on(...)` |
| Event store | Axon (JDBC/embedded) | Bảng `stored_events` tự viết |
| Saga | `@Saga` + `SagaLifecycle` | `@Component` + try/catch + compensation tgường minh |
| Command/Query bus | `CommandGateway`/`QueryGateway` | `*CommandService` / `*QueryService` |
| Optimistic concurrency | Axon tự xử lý | Tự kiểm tra `maxVersion` |
| Phụ thuộc mạng | Cần Maven Central (bị chặn ở 1 số env) | Build offline OK |
| Độ phức tạp hiểu | Thấp khi dùng, khó hiểu bên trong | Rõ ràng từng bước, nhiều code hạ tầng hơn |
