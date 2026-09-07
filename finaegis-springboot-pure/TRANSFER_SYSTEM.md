# Hệ thống TRANSFER trong Finaegis Core Banking

> Tài liệu giải thích chi tiết toàn bộ hệ thống chuyển tiền (Transfer) của project, từ lớp HTTP cho tới lớp Event Sourcing logic.

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Danh sách file](#2-danh-sách-file)
3. [Luồng hoạt động end-to-end](#3-luồng-hoạt-động-end-to-end)
4. [Đào sâu từng thành phần](#4-đào-sâu-từng-thành-phần)
5. [Các khái niệm cốt lõi](#5-các-khái-niệm-cốt-lõi)
6. [Full luồng TRANSFER — Trace chi tiết](#6-full-luồng-transfer--trace-từng-bước-chi-tiết)
7. [FAQ / Điểm mấu chốt](#7-faq--điểm-mấu-chốt)

---

## 1. Tổng quan kiến trúc

Hệ thống Transfer xử lý **giao dịch chuyển tiền phân tán** (giữa 2 tài khoản khác nhau) dựa trên 3 mô hình kết hợp:

| Mô hình | Vai trò |
|---------|---------|
| **Event Sourcing** | Trạng thái giao dịch = chuỗi sự kiện (events), không lưu snapshot DB trực tiếp |
| **CQRS** | Tách biệt nơi **ghi** (write side / event store) và nơi **đọc** (read side / projection table) |
| **Saga Pattern** | Điều phối giao dịch nhiều bước với cơ chế **bù trừ (compensation)** |

### Các đối tượng chính

- **Transfer** — aggregate (đơn vị domain) quản lý vòng đời 1 lần chuyển tiền.
- **Account** — aggregate đối tác, quản lý số dư của tài khoản (được debit/credit bởi saga).
- **TransferView** — bảng read-model (projection) để query nhanh.
- **EventStore** — nơi persist + publish mọi domain event.

### State machine của Transfer

```
┌─────────┐   initiate   ┌─────────┐   complete   ┌───────────┐
│ (new)   │ ───────────▶ │ PENDING │ ───────────▶ │ COMPLETED │
└─────────┘              └─────────┘                              │
                              │ fail                              │
                              ▼                                   │
                         ┌─────────┐                              │
                         │ FAILED  │ ◀────────────────────────────┘
                         └─────────┘
```

---

## 2. Danh sách file

| File | Lớp | Package |
|------|-----|---------|
| `TransferController.java` | REST Controller (đầu vào HTTP) | `presentation.rest` |
| `TransferApplicationService.java` | Application Service (điều phối) | `domain.payment.model` |
| `Transfer.java` | Aggregate (Event Sourcing core) | `domain.payment.aggregate` |
| `TransferSaga.java` | Saga (giao dịch phân tán) | `domain.payment.saga` |
| `TransferProjection.java` | Projection (CQRS read side) | `domain.payment.model` |
| `TransferView.java` | JPA Entity (read-model) | `domain.payment.model` |
| `TransferViewRepository.java` | JPA Repository | `domain.payment.model` |
| `TransferInitiatedEvent.java` | Domain Event | `domain.payment.event` |
| `TransferCompletedEvent.java` | Domain Event | `domain.payment.event` |
| `TransferFailedEvent.java` | Domain Event | `domain.payment.event` |
| `AggregateRoot.java` | Base class Event Sourcing | `eventstore` |
| `DomainEvent.java` | Base class Domain Event | `domain.common` |
| `AggregateRepository.java` | Repository generic cho aggregate | `eventstore` |
| `EventStore.java` | Persist + publish event | `eventstore` |
| `StoredEvent.java` | JPA Entity (event store) | `eventstore` |
| `EventPublisher.java` | Interface publish event | `eventstore` |
| `AccountCommandService.java` | Command service cho Account | `domain.account.command` |
| `Account.java` | Aggregate Account | `domain.account.aggregate` |

---

## 3. Luồng hoạt động end-to-end

Khi client gọi `POST /api/v1/transfers`:

```
POST /api/v1/transfers
  │
  ▼ TransferController.initiate()
  │   (kiểm tra quyền TRANSFER_INITIATE)
  │
  ▼ TransferApplicationService.initiate()
  │   - transferId = UUID()
  │   - load aggregate Transfer (rỗng vì mới)
  │   - transfer.initiate(...)  → validate → apply(TransferInitiatedEvent)
  │   - aggregateRepository.save(transfer, version=0)  → PERSIST + PUBLISH
  │
  │   EventStore.append()  ── optimistic lock + INSERT stored_events ──┐
  │   publishAll()  ───────────────────────────────────────────────────┤
  │                                                                    ▼
  │   HTTP 202 ACCEPTED (trả về ngay cho client)        ┌──────────────────────┐
  │  (xử lý thực sự diễn ra bất đồng bộ phía sau)        │    SAGA bắt đầu       │
  │                                                      └────────────┬─────────┘
  │                                                                   │
  │       TransferProjection (CQRS)                     TransferSaga (giao dịch phân tán)
  │       - INSERT transfer_view (PENDING)              - B1: withdraw(tài khoản gửi)
  │                                                       - B2: deposit(tài khoản nhận)
  │                                                       - B3: complete(transfer)
  │                                                          └─▶ publish CompletedEvent
  │                                                       - Nếu lỗi: compensate → FAILED
```

---

## 4. Đào sâu từng thành phần

### 4.1 `TransferController.java`

REST controller, base path `/api/v1/transfers`.

**Endpoints:**

| Method | Path | Quyền | Mô tả |
|--------|------|-------|-------|
| `POST` | `/` | `TRANSFER_INITIATE` | Khởi tạo chuyển tiền |
| `GET` | `/{transferId}` | `TRANSFER_VIEW` | Xem chi tiết transfer |
| `GET` | `/?accountId=` | `TRANSFER_VIEW` | Liệt kê transfer của tài khoản |

**Ví dụ endpoint initiate:**

```java
@PostMapping
@PreAuthorize("hasAuthority('" + PermissionConstants.TRANSFER_INITIATE + "')")
public ResponseEntity<Void> initiate(@RequestBody TransferRequest request) {
    transferService.initiate(
        request.fromAccountId(), request.toAccountId(),
        request.amount(), request.currency(),
        request.description(), request.initiatedBy());
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
}
```

- `@PreAuthorize` — Spring Security kiểm tra authority `TRANSFER_INITIATE`, thiếu → **403 Forbidden**, method không chạy.
- `@RequestBody` — Spring deserialize JSON sang record `TransferRequest`.
- `HttpStatus.ACCEPTED` (202) — báo "đã chấp nhận, đang xử lý bất đồng bộ" (do saga chạy sau), phù hợp với kiến trúc **eventual consistency**.

---

### 4.2 `TransferApplicationService.java`

Application Service (tầng điều phối). `@Service`, inject `AggregateRepository` + `TransferViewRepository`.

```java
public String initiate(String fromAccountId, String toAccountId, BigDecimal amount,
                       String currency, String description, String initiatedBy) {
    String transferId = UUID.randomUUID().toString();          // 1. sinh ID
    Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new); // 2. load rỗng
    transfer.initiate(...);                                    // 3. command + validate
    aggregateRepository.save(transfer, transfer.getVersion()); // 4. persist + publish
    return transferId;
}

public TransferView get(String transferId) { ... }             // đọc từ read-model (CQRS)
public List<TransferView> listByAccount(String accountId) { ... } // gộp 2 chiều from/to
```

> Lưu ý CQRS: phần **đọc** lấy dữ liệu từ `transfer_view` (bảng read-model) chứ **không** replay events — hiệu năng cao.

---

### 4.3 `Transfer.java` — Aggregate (Event Sourcing core)

`public class Transfer extends AggregateRoot`

- `TYPE = "com.finaegis.domain.payment"` — chuỗi discriminator để resolve class event khi replay.
- `enum Status { PENDING, COMPLETED, FAILED }`.

**Command methods:**

```java
public void initiate(...) {
    if (fromAccountId.equals(toAccountId))                        // validate: cùng tài khoản
        throw new IllegalArgumentException("Cannot transfer to the same account");
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) // validate: amount > 0
        throw new IllegalArgumentException("Transfer amount must be positive");
    apply(new TransferInitiatedEvent(...));                       // tạo event
}

public void complete() {
    if (status != Status.PENDING) return;   // idempotent
    apply(new TransferCompletedEvent());
}

public void fail(String reason) {
    if (status == Status.FAILED || status == Status.COMPLETED) return; // idempotent
    apply(new TransferFailedEvent(reason));
}
```

**Event handlers (replay state):** `when(event)` gọi handler qua reflection:

```java
void on(TransferInitiatedEvent event) { 
    // set các field ...; this.status = Status.PENDING; 
}
void on(TransferCompletedEvent event) { this.status = Status.COMPLETED; }
void on(TransferFailedEvent event)    { this.status = Status.FAILED; this.failureReason = event.reason; }
```

> **Điểm mấu chốt Event Sourcing:** handler `on()` được gọi cả khi command áp dụng lẫn khi **replay từ event store** (lúc server khởi động lại). Trạng thái luôn được *tái dựng* từ chuỗi events, không lưu trực tiếp.

---

### 4.4 Các Event Classes

Kế thừa `DomainEvent` (chứa metadata: `aggregateId`, `aggregateType`, `version`, `occurredAt`, `eventId`).

- **`TransferInitiatedEvent`** — payload: `fromAccountId, toAccountId, amount, currency, description, initiatedBy`. Kích hoạt Saga (bước 1).
- **`TransferCompletedEvent`** — `completedAt = Instant.now()`.
- **`TransferFailedEvent`** — `reason` (lý do thất bại).

---

### 4.5 `TransferSaga.java` — Distributed Transaction Coordinator (quan trọng nhất)

`@Component` + `@EventListener` — tự động kích hoạt khi `TransferInitiatedEvent` được publish.

```java
@EventListener
@Transactional
public void on(TransferInitiatedEvent event) {
    String transferId = event.getAggregateId();
    try {
        // B1: debits tài khoản gửi
        accountCommandService.withdraw(event.fromAccountId, event.amount, event.currency,
                                       reference("DEBIT", transferId));
        // B2: credit tài khoản nhận
        accountCommandService.deposit(event.toAccountId, event.amount, event.currency,
                                      reference("CREDIT", transferId));
        // B3: hoàn tất transfer
        Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new);
        transfer.complete();
        aggregateRepository.save(transfer, transfer.getVersion());
    } catch (Exception ex) {
        compensate(event, transferId, ex);  // bù trừ
        throw ex;                           // rollback toàn transaction
    }
}
```

**Compensation** (khi có bước fail):

```java
private void compensate(TransferInitiatedEvent event, String transferId, Exception cause) {
    // đảo debit: trả tiền lại tài khoản GỬI
    accountCommandService.deposit(event.fromAccountId, event.amount, event.currency,
                                  reference("COMPENSATE", transferId));
    // đánh dấu transfer FAILED (publish TransferFailedEvent → projection cập nhật)
    Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new);
    transfer.fail(cause.getMessage());
    aggregateRepository.save(transfer, transfer.getVersion());
}
```

**Ví dụ lỗi:** Debit tài khoản gửi thành công, nhưng deposit tài khoản nhận thất bại (vd bị đóng băng → `requireActive()` throw). Nếu dừng lại, tiền bị trừ mà người nhận không nhận được → **mất tiền**. Compensation **hoàn tiền** về tài khoản gửi, rồi đánh dấu `FAILED`. Đây là nguyên tắc **compensating transaction** — đảm bảo *eventual consistency*.

---

### 4.6 `TransferProjection.java` — CQRS read side

`@Component` + `@EventListener` — tự động cập nhật read-model khi event published.

```java
on(TransferInitiatedEvent)  → INSERT transfer_view with status=PENDING
on(TransferCompletedEvent)  → UPDATE status=COMPLETED
on(TransferFailedEvent)     → UPDATE status=FAILED
```

---

### 4.7 `TransferView.java` + `TransferViewRepository.java` — Read-Model

- `@Entity @Table(name="transfer_view")` — bảng JPA denormalized chỉ để query nhanh.
- `amount`: `precision=20, scale=8` (tới 8 số thập phân).
- `TransferViewRepository` extends `JpaRepository` với các query method tự sinh:
  - `findByFromAccountId` / `findByToAccountId` / `findByStatus`.

---

### 4.8 `AggregateRoot.java` — Base class Event Sourcing

| Method | Chức năng |
|--------|-----------|
| `apply(DomainEvent)` | Set aggregate metadata, thêm vào `changes`, gọi `when()` |
| `when(DomainEvent)` | Reflection tìm `on(<EventClass>)` và gọi handler |
| `rebuildFrom(stored, mapper)` | Replay toàn bộ events → tái dựng trạng thái |
| `deserialize(stored, mapper)` | `Class.forName(type + ".event." + eventType)` resolve class |
| `getUncommittedChanges()` / `clearChanges()` | Quản lý danh sách event chưa lưu |
| `getVersion()` / `setVersion()` | Optimistic concurrency |

---

### 4.9 `AggregateRepository.java` — Cầu nối Event Store

```java
// LOAD: stream rỗng → factory tạo mới; có event → replay (rebuildFrom)
load(streamId, type, factory)

// SAVE: ghi từng uncommitted event với version tăng dần (optimistic lock),
// rồi publishAll(changes) → kích hoạt Saga + Projection, cuối cùng clearChanges()
save(aggregate, expectedVersion)
```

---

### 4.10 `EventStore.java` — Persist + publish

**`append()`** — bảo vệ optimistic concurrency:

```java
long current = repository.maxVersion(streamId);           // version mới nhất từ DB
if (current != expectedVersion)                            // khác → xung đột
    throw new ConcurrentModificationException("Optimistic lock violated...");
long newVersion = current + 1;
...
StoredEvent stored = StoredEvent.create(streamId, aggregateId, aggregateType,
                                         event.getEventType(), newVersion, json);
repository.save(stored);          // INSERT vào stored_events
event.markRecorded();             // đánh dấu đã ghi (quan trọng cho publish)
```

**`publishAll()`** — chỉ publish event đã ghi thành công (`isRecorded()`).

---

### 4.11 `AccountCommandService.java` + `Account.java`

Dùng bởi saga để debit/credit:

```java
public void withdraw(accountId, amount, currency, reference) {
    Account account = aggregateRepository.load(accountId, Account.TYPE, Account::new);
    account.withdraw(amount, currency, reference);   // validate: đủ tiền? không frozen/closed?
    aggregateRepository.save(account, account.getVersion());
}
```

Trong `Account.withdraw()` (`Account.java:52`) — **kiểm tra quyết định**:

```java
if (balance.compareTo(amount) < 0) {
    throw new IllegalStateException("Insufficient funds: balance " + balance + " < requested " + amount);
}
apply(new MoneyWithdrawnEvent(...));   // balance = balance.subtract(amount)
```

> `Account.balance` là **derived state** — được tính bằng cách replay `MoneyDepositedEvent`/`MoneyWithdrawnEvent`, không lưu balance riêng.
> Lỗi "không đủ tiền" → throw → saga bắt → compensation.

---

## 5. Các khái niệm cốt lõi

### 5.1 Event Sourcing
- Trạng thái = chuỗi events (+ trạng thái derived), **source of truth** là bảng `stored_events`.
- Khi khởi động/server restart, replay toàn bộ events để tái dựng trạng thái (`rebuildFrom`).
- Không sửa/xóa event — chúng bất biến (immutable), mọi thay đổi là thêm event mới.

### 5.2 CQRS (Command Query Responsibility Segregation)
- **Write side:** Event Store — chỉ ghi event (command).
- **Read side:** `transfer_view` — query nhanh, được projection duy trì từ events.
- Lợi ích: query không đụng tới write model, scale độc lập, đọc nhanh.

### 5.3 Saga Pattern
- Điều phối **distributed transaction** giữa nhiều aggregates (2 tài khoản) không thể gói trong 1 DB transaction duy nhất.
- **Orchestration:** saga điều khiển từng bước (B1 debit → B2 credit → B3 complete).
- **Compensation:** nếu bất kỳ bước nào fail → thực hiện bù trừ (hoàn tiền) rồi đánh dấu FAILED.
- Kết quả: **eventual consistency** — hệ thống không nhất quán tức thời nhưng sẽ nhất quán cuối cùng.

### 5.4 Optimistic Concurrency
- Mỗi event có `event_version` tăng dần trong 1 stream.
- Khi ghi, so sánh `maxVersion` trong DB với `expectedVersion` truyền vào; khác nhau → `ConcurrentModificationException`.
- Ngăn **lost update** (2 request ghi đè cùng lúc).

### 5.5 UUID làm ID đa vai trò
`transferId` (UUID) đóng 3 vai trò:
1. `aggregateId` — ID của aggregate `Transfer`.
2. `streamId` — nhóm các event trong event store.
3. Khóa chính của row trong `transfer_view`.

---

## 6. Full luồng TRANSFER — Trace từng bước chi tiết

Phần này mô phỏng **toàn bộ vòng đời** của một lần chuyển tiền, theo dõi sát từng dòng code thực tế, kèm dữ liệu minh họa ở cả 2 kịch bản: **thành công** và **thất bại**.

### 6.1 Chuyện gì thực sự xảy ra trên đường truyền (HTTP → Controller)

**Request từ client:**

```http
POST /api/v1/transfers
Content-Type: application/json
Authorization: Bearer <JWT của user có quyền TRANSFER_INITIATE>

{
  "fromAccountId": "acc-111",
  "toAccountId":   "acc-222",
  "amount":        100.50,
  "currency":      "USD",
  "description":   "Thanh toan hoa don thang 9",
  "initiatedBy":   "user-1"
}
```

**Bước 1 — Spring Security filter:**
JWT được giải mã → lấy ra danh sách `authorities` của user. Nếu user **không** có `TRANSFER_INITIATE`, `@PreAuthorize` chặn ngay → trả **403 Forbidden** mà **không vào method**.

**Bước 2 — Jackson deserialize `@RequestBody`:**
Spring dùng `ObjectMapper` map JSON → record `TransferRequest`. Các field phải khớp tên (case-sensitive). Nếu thiếu field bắt buộc hoặc sai kiểu → **400 Bad Request**.

**Bước 3 — gọi service, trả 202:**
Controller gọi `transferService.initiate(...)` rồi trả `ResponseEntity.status(ACCEPTED).build()` → **HTTP 202** ngay lập tức, không chờ saga.

---

### 6.2 Trace KỊCH BẢN THÀNH CÔNG

Giả định ban đầu:
- Tài khoản `acc-111` có balance = `500.00 USD` (được tái dựng từ các event trong quá khứ).
- Tài khoản `acc-222` có balance = `0.00 USD`.

#### Bước A — `TransferApplicationService.initiate()`

```java
String transferId = UUID.randomUUID().toString();   // ví dụ: "9a1b2c3d-..."
Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new);
```

- `load` gọi `eventStore.loadStream("9a1b2c3d...")` → `findByStreamIdOrderByEventVersionAsc` → **rỗng** → factory tạo `Transfer` trống, `version=0`, `changes=[]`.

```java
transfer.initiate("acc-111", "acc-222", 100.50, "USD", "...", "user-1");
```

- Vượt qua 2 validation:
  - `"acc-111".equals("acc-222")` → **false** (khác nhau).
  - `100.50 > 0` → **true**.
- `apply(new TransferInitiatedEvent(...))`:
  - Event được set `aggregateId = "9a1b2c3d..."`, `aggregateType = "com.finaegis.domain.payment"`.
  - `changes = [TransferInitiatedEvent]`.
  - `when(event)` → reflection gọi `on(TransferInitiatedEvent)` → đặt `status = PENDING` (RAM).
  
```java
aggregateRepository.save(transfer, transfer.getVersion());  // save(transfer, 0)
```

- Đi vào `EventStore.append(..., expectedVersion=0, ...)`:
  - `maxVersion("9a1b2c3d...")` → query `SELECT COALESCE(MAX(event_version),0)...` → **0**.
  - `0 == 0` → **không có xung đột optimistic lock** ✓.
  - `newVersion = 1`.
  - Serialize event thành JSON, lưu `StoredEvent` vào bảng `stored_events`:
    ```
    stream_id="9a1b2c3d...", aggregate_type="com.finaegis.domain.payment",
    event_type="TransferInitiatedEvent", event_version=1
    event_data={"fromAccountId":"acc-111","toAccountId":"acc-222","amount":100.50,"currency":"USD",...}
    ```
  - `event.markRecorded()` → `recorded = true`.
- `publishAll(changes)` → `SpringEventPublisher.publish(event)` → **`publisher.publishEvent(event)`** → đưa event vào **Spring event bus**.

> Ngay lúc này, tất cả `@EventListener` quan tâm đến `TransferInitiatedEvent` được gọi (đồng bộ, trong cùng transaction). Có 2 listener: `TransferSaga` và `TransferProjection`.

#### Bước B — `TransferProjection` tạo row read-model (PENDING)

```java
on(TransferInitiatedEvent event) {
    repository.save(TransferView.builder()
        .transferId("9a1b2c3d...")
        .fromAccountId("acc-111")
        .toAccountId("acc-222")
        .amount(100.50)
        .currency("USD")
        .description("...")
        .initiatedBy("user-1")
        .status("PENDING")
        .build());
}
```

Bảng `transfer_view` giờ có 1 row mới với `status = "PENDING"`.

#### Bước C — `TransferSaga` Bước 1: Debit tài khoản gửi

```java
accountCommandService.withdraw("acc-111", 100.50, "USD", "TRANSFER-DEBIT-9a1b2c3d...");
```

Trong `AccountCommandService.withdraw()`:
1. `aggregateRepository.load("acc-111", Account.TYPE, Account::new)`:
   - stream của `acc-111` **không rỗng** (đã tồn tại) → gọi **`rebuildFrom`**.
   - **Replay toàn bộ lịch sử events của `acc-111`** (AccountCreated, các lần deposit/withdraw trước) → sau khi replay, `balance = 500.00 USD` trong RAM.
   - *Đay chính là lý do balance không lưu thẳng trong DB — nó được tính lại từ events.*
2. `account.withdraw(100.50, "USD", ref)`:
   - `requireActive()` → không frozen, không closed → pass.
   - `balance (500.00) >= amount (100.50)` → **đủ tiền** → không throw.
   - `apply(new MoneyWithdrawnEvent(100.50, "USD", ref))` → `changes` có event → `on()` update `balance = 399.50` (RAM).
3. `aggregateRepository.save(account, version)`:
   - Optimistic lock pass → persist `MoneyWithdrawnEvent` (version tăng 1) → publish event.

> Sau bước C: `acc-111` đã bị **trừ** 100.50, balance mới = **399.50** (lưu dạng derived qua event).

#### Bước D — `TransferSaga` Bước 2: Credit tài khoản nhận

```java
accountCommandService.deposit("acc-222", 100.50, "USD", "TRANSFER-CREDIT-9a1b2c3d...");
```

1. Load + replay `acc-222` → `balance = 0.00`.
2. `account.deposit(100.50, ...)`:
   - `requireActive()` → pass.
   - amount > 0 → pass.
   - `apply(new MoneyDepositedEvent(...))` → `on()` cập nhật `balance = 100.50`.
3. Persist `MoneyDepositedEvent` + publish.

> Sau bước D: `acc-222` được **cộng** 100.50, balance mới = **100.50**.

#### Bước E — `TransferSaga` Bước 3: Hoàn tất transfer

```java
Transfer transfer = aggregateRepository.load("9a1b2c3d...", Transfer.TYPE, Transfer::new);
// Lần load này KHÔNG rỗng: replay TransferInitiatedEvent -> status = PENDING (nếu server restart,
// trạng thái vẫn khôi phục được nhờ replay!)
transfer.complete();    // status PENDING -> apply(TransferCompletedEvent) -> COMPLETED
aggregateRepository.save(transfer, ...);   // persist + publish TransferCompletedEvent
```

- `TransferCompletedEvent` được publish → `TransferProjection` bắt → **UPDATE** `transfer_view` → `status = "COMPLETED"`.

#### Kết quả kịch bản thành công

| Nơi | Trạng thái cuối |
|-----|-----------------|
| `acc-111` | balance từ `500.00` → `399.50` (event: MoneyWithdrawn) |
| `acc-222` | balance từ `0.00` → `100.50` (event: MoneyDeposited) |
| Transfer stream | events: `Initiated(1)` → `Completed(2)` |
| `transfer_view` row | `status = COMPLETED` |
| HTTP response | `202 Accepted` (đã trả từ đầu, không chờ) |

> **Không có transaction DB nào đơn lẻ bao trùm toàn bộ** — 3 event được ghi ở 3 lần `save` riêng biệt, nhưng tất cả nằm trong `@Transactional` của saga nên vẫn nhất quán. Nếu crash giữa chừng, replay events vẫn cho kết quả đúng.

---

### 6.3 Trace KỊCH BẢN THẤT BẠI — hết tiền / tài khoản nhận lỗi

Giả định: `acc-111` balance = `50.00 USD` nhưng người dùng cố chuyển `500 USD`.

**Cùng các bước A & B** (initiate PENDING + projection tạo row).

#### Bước C' — Debit tài khoản gửi **THẤT BẠI**

Trong `account.withdraw(500, ...)`:
- `requireActive()` → pass (tài khoản hoạt động).
- `500 > 0` → pass.
- `balance (50.00) < amount (500)` → **`throw new IllegalStateException("Insufficient funds...")`**.

**Luồng lỗi lan truyền:**
1. Exception từ `account.withdraw()` → phá vỡ `AccountCommandService.withdraw()`.
2. → phá vỡ `accountCommandService.withdraw()` trong `TransferSaga.on()`.
3. → rơi vào **`catch (Exception ex)`** trong saga.
4. Saga gọi `compensate(event, transferId, ex)`.
5. Saga **`throw ex`** → `@Transactional` của saga **rollback**.

#### Bước compensate — nhưng debit đã fail nên KHÔNG có tiền nào bị trừ

```java
private void compensate(...) {
    // Vì bước debit fail (chưa trừ tiền), deposit hoàn lại sẽ:
    accountCommandService.deposit("acc-111", 500.00, "USD", "TRANSFER-COMPENSATE-...");
    // NOTE: acc-111 chỉ có 50, nhưng deposit VIEW như "bù" — thực tế debit chưa chạy nên
    //       compensation này chỉ là phòng thủ. Balance acc-111 KHÔNG bị đổi (vẫn 50).
    ...
    Transfer transfer = aggregateRepository.load(transferId, ...);
    transfer.fail("Insufficient funds: balance 50.00 < requested 500");  // PENDING -> FAILED
    aggregateRepository.save(transfer, ...);   // persist + publish TransferFailedEvent
}
```

> **Quan trọng:** Vì `withdraw` fail ngay (trước khi bất kỳ event nào của account được persist), `MoneyWithdrawnEvent` **chưa bao giờ được ghi** → không có gì để hoàn lại. Compensation "no-op" về mặt số dư, nhưng đảm bảo transfer được đánh dấu **FAILED**.

- `TransferFailedEvent(reason)` được publish → `TransferProjection` → **UPDATE** `transfer_view` → `status = "FAILED"`.

#### Kịch bản khác: debit thành công nhưng credit thất bại

Giả định `acc-111` đủ tiền, nhưng `acc-222` đang **bị đóng băng (frozen)**:
1. **Bước C**: debit `acc-111` ✓ (đã trừ 500, balance giảm).
2. **Bước D**: `account.deposit("acc-222", ...)` → bên trong gọi `requireActive()` → `acc-222` frozen → **throw**.
3. → `catch` trong saga.
4. **compensate**: gọi `deposit("acc-111", 500, ...)` → **hoàn tiền** lại `acc-111` (balance được cộng lại 500, khôi phục giá trị ban đầu). ✓
5. Đánh dấu transfer **FAILED**.

> Đây chính là **compensating transaction**: nếu debit đã thực hiện mà credit fail, ta **đảo ngược** debit (refund) để không làm mất tiền của người gửi. Đảm bảo *eventual consistency*.

| Trường hợp | Balance acc-111 sau đó | Kết quả |
|------------|------------------------|---------|
| Debit fail (hết tiền) | không đổi (chưa trừ) | FAILED |
| Debit ok, credit fail (frozen) | **được hoàn lại** về ban đầu | FAILED |
| Mọi bước ok | bị trừ đúng | COMPLETED |

---

### 6.4 Bản đồ full event flow (cả 2 bên)

```
POST /api/v1/transfers
  │
  ├─▶ EventStore.append: TransferInitiatedEvent (stream version 1)
  │        └─▶ publish ──┬─▶ TransferProjection → INSERT transfer_view (PENDING)
  │                      └─▶ TransferSaga
  │                             │
  │                             ├─ withdraw("acc-111") ──► Account events (MONEY WITHDRAWN)
  │                             │        └─▶ publish (projection account riêng)
  │                             ├─ deposit("acc-222")  ──► Account events (MONEY DEPOSITED)
  │                             │        └─▶ publish (projection account riêng)
  │                             └─ complete() ──► EventStore.append: TransferCompletedEvent
  │                                      └─▶ publish ──► TransferProjection → UPDATE = COMPLETED
  │
  │  (nếu có lỗi ở bất kỳ bước nào)
  │        └─▶ compensate() ──► EventStore.append: TransferFailedEvent
  │                                └─▶ publish ──► TransferProjection → UPDATE = FAILED
  │
  ▼
HTTP 202 ACCEPTED (trả ngay, saga chạy ngầm)
```

---

### 6.5 Cơ chế Publish — Spring event bus

`EventPublisher` được cài đặt bởi **`SpringEventPublisher`** (`eventstore/SpringEventPublisher.java`):

```java
@Component
public class SpringEventPublisher implements EventPublisher {
    private final ApplicationEventPublisher publisher;
    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);   // đẩy vào Spring event bus
    }
}
```

- `publisher.publishEvent(event)` đưa `TransferInitiatedEvent`/`TransferCompletedEvent`/... vào **Spring ApplicationContext event bus**.
- Bất kỳ bean nào có `@EventListener` với parameter đúng kiểu event sẽ được gọi **đồng bộ** (trong cùng thread/transaction).
- Đây là cơ chế **loose coupling**: saga/projection không cần biết nhau, chỉ cần lắng nghe event.

---

### 6.6 Cơ chế Serialization / Deserialization khi replay

**Lưu (persist)** — `EventStore.writeEventData()`:
```java
String json = objectMapper.writeValueAsString(event);   // toàn bộ public field + (metadata bị @JsonIgnore loại bỏ)
```

**Đọc (replay)** — `AggregateRoot.deserialize()`:
```java
Class<?> clazz = Class.forName(stored.getAggregateType() + ".event." + stored.getEventType());
// ví dụ: "com.finaegis.domain.payment" + ".event." + "TransferInitiatedEvent"
DomainEvent event = (DomainEvent) mapper.readValue(stored.getEventData(), clazz);
event.setAggregateId(...); event.setAggregateType(...); event.setVersion(...); event.markRecorded();
```

**Điều kiện quan trọng:**
- `aggregateType` lưu trong DB phải **khớp chính xác** package path thực tế (vì dùng `Class.forName`).
- `eventType` (tên class đơn giản) phải là tên đúng của class event.
- Event class phải có **constructor rỗng** (`@NoArgsConstructor`) để Jackson deserialize.

---

### 6.7 `aggregateRepository.save(transfer, transfer.getVersion())` — để làm gì?

Đây là câu lệnh **"chốt"** toàn bộ quy trình: biến state trong RAM thành dữ liệu bền vững trong Event Store và **kích hoạt saga + projection**. Không có nó, transfer chỉ tồn tại trong bộ nhớ và không bao giờ được xử lý.

**Bối cảnh 3 bước trong `initiate()`:**

```java
Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new);   // (1) tạo rỗng trong RAM
transfer.initiate(...);                                                                     // (2) command: apply(event) → chỉ vào RAM
aggregateRepository.save(transfer, transfer.getVersion());                                  // (3) PERSIST + PUBLISH ← quan trọng
```

- **(1)** `load` — aggregate rỗng, `version = 0`, **CHƯA có gì trong DB**.
- **(2)** `initiate` — `apply(event)` chỉ thêm event vào `changes` (RAM) + cập nhật state object. **CHƯA ghi DB**.
- **(3)** `save` — **thực sự ghi xuống Event Store** + **publish**. Đây là bước bắt buộc.

**Mổ xẻ `save()` (`AggregateRepository.java:48`):**

```java
public <T extends AggregateRoot> void save(T aggregate, long expectedVersion) {
    aggregate.setVersion(expectedVersion);                          // (a) lưu version cũ
    List<DomainEvent> changes = aggregate.getUncommittedChanges();  // (b) = [TransferInitiatedEvent]
    if (changes.isEmpty()) return;                                  // (c) không có gì đổi → thoát
    for (DomainEvent event : changes) {                             // (d)
        long next = aggregate.getVersion() + 1;                     // next = 1
        eventStore.append(streamId, aggregateId, aggregateType,
                          aggregate.getVersion(), event);           // (e) PERSIST + optimistic lock
        aggregate.setVersion(next);                                 // (f)
    }
    eventStore.publishAll(changes);                                 // (g) PUBLISH → kích hoạt saga/projection
    aggregate.clearChanges();                                       // (h) xóa khỏi RAM
}
```

| Dòng | Vai trò |
|------|---------|
| (a) | Ghi nhớ `expectedVersion` — dùng để kiểm tra optimistic lock. |
| (b) | Lấy các event chưa lưu. |
| (c) | Không có gì thay đổi → thoát sớm, tránh ghi rác. |
| (e) | **INSERT event vào bảng `stored_events`** (serialize JSON, version tăng dần, kiểm tra lock). |
| (g) | **Publish** → Spring event bus → Saga + Projection chạy. |
| (h) | Dọn `changes` trong RAM (đã lưu xong). |

**3 việc chính mà `save()` thực hiện:**

1. **Persist event xuống Event Store** — Event Sourcing không lưu trạng thái `Transfer`, mà lưu *sự kiện đã xảy ra* (dạng JSON, có version).
2. **Bảo vệ Optimistic Concurrency** — trong `EventStore.append()`:
   ```java
   long current = repository.maxVersion(streamId);
   if (current != expectedVersion)
       throw new ConcurrentModificationException("Optimistic lock violated...");
   ```
   Nếu ai đó đã ghi thêm event vào cùng stream → `current != expected` → throw → **ngăn ghi đè (lost update)**.
3. **Publish event → kích hoạt hậu cảnh** — `publishAll` → `publisher.publishEvent` → `TransferSaga` bắt đầu debit/credit, `TransferProjection` ghi read-model.

**Vì sao phải truyền `transfer.getVersion()`?**
- Đó là *expectedVersion* — phiên bản bạn *nghĩ* aggregate đang ở.
- Giúp `EventStore.append()` biết event đầu nên bắt đầu từ version nào (transfer mới: `0` → event đầu `version 1`).
- Là cơ chế **khóa lạc quan**: nếu DB đã đổi version → báo lỗi xung đột.

**Ví dụ cụ thể:**
- Trước `save`: DB trống, `changes=[TransferInitiatedEvent]`, `version=0`.
- `save(transfer, 0)`:
  - `append`: `maxVersion(0)==expected(0)` ✓ → persist event `version=1`.
  - publish `TransferInitiatedEvent` → saga + projection chạy.
  - `clearChanges()` → `changes=[]`, `version=1`.
- Sau này saga gọi `save(transfer, transfer.getVersion())` để lưu `TransferCompletedEvent` / `TransferFailedEvent` — cùng cơ chế, version tăng lên 2, 3,...

> **Tóm tắt 1 câu:** `aggregateRepository.save(...)` = *"đưa những sự kiện vừa tạo trong bộ nhớ vào Event Store (kèm chống xung đột) rồi publish để kích hoạt Saga và Projection"* — biến một command "ảo" trong RAM thành dữ liệu bền vững và trigger toàn bộ quy trình.

---

## 7. FAQ / Điểm mấu chốt

**Q: Tại sao trả về HTTP 202 thay vì 200/201?**
A: Vì chuyển tiền thực sự diễn ra bất đồng bộ ở Saga sau đó. 202 = "đã chấp nhận, chưa xử lý xong" — đúng bản chất eventual consistency.

**Q: Tại sao không lưu `balance` trực tiếp trong DB?**
A: Event Sourcing. `balance` là derived state từ chuỗi events. Điều này cho phép replay lịch sử, audit, và không có "trạnh thái không nhất quán" giữa DB và business logic.

**Q: Chuyện gì xảy ra nếu hết tiền khi transfer?**
A: `Account.withdraw()` throw `IllegalStateException` → saga catch → compensation (không cần hoàn tiền vì chưa trừ) → đánh dấu transfer FAILED.

**Q: Chuyện gì nếu credit tài khoản nhận fail sau khi debit thành công?**
A: Compensation hoàn tiền lại tài khoản gửi (refund), rồi đánh dấu FAILED. Tránh mất tiền.

**Q: Tại sao `Transfer.TYPE` phải khớp package path?**
A: Vì `AggregateRoot.deserialize()` dùng `Class.forName(aggregateType + ".event." + eventType)` để resolve class. Sai path → không đọc/gọi được handler khi replay.

**Q: hệ thống xử lý 2 request chuyển tiền cùng lúc thế nào?**
A: Mỗi transfer có UUID riêng (không xung đột). Optimistic lock trên từng stream bảo vệ ghi. Các aggregate khác nhau độc lập.

---

### 7.1 "Tại sao cần" trong luồng transfer — Câu hỏi & trả lời chi tiết

**Q1: Tại sao cần `UUID.randomUUID()` để tạo `transferId`?**
A: Vì `transferId` đóng **3 vai trò** (aggregateId, streamId, khóa chính read-model) nên phải **duy nhất toàn cục**. UUID không cần DB sequence, không bị xung đột khi nhiều transfer tạo đồng thời, và an toàn trên nhiều node/server (không có "single point" sinh số).

**Q2: Tại sao phải gọi `load(...)` rồi mới `initiate(...)`?**
A: Vì để aggregate tự tái dựng trạng thái từ lịch sử events (nếu đã tồn tại) trước khi áp dụng command. Design này đảm bảo command luôn được thực thi trên **trạng thái hiện tại đúng** — không bao giờ "mù" về dữ liệu. Với transfer mới, `load` trả aggregate rỗng để bắt đầu ghi nhận event đầu tiên.

**Q3: Tại sao validate ở trong aggregate (`Transfer.initiate`) thay vì ở controller?**
A: Vì business rule (không chuyển vào cùng tài khoản, amount > 0) là **bất biến của domain** — phải luôn được đảm bảo, bất kể được gọi từ đâu (API, service khác, saga về sau). Đặt trong aggregate khiến rule **không thể bị bỏ qua**; controller chỉ lo về HTTP/quyền.

**Q4: Tại sao `apply()`, `when()`, `on()` tách rời nhau?**
A: Vì chúng phục vụ 2 mục đích khác nhau:
- `apply()` = ghi nhận sự kiện mới + dùng trong command.
- `when()`/`on()` = áp dụng sự kiện lên state, **dùng chung cho cả command lẫn replay**.
Tách ra giúp **một bộ handler duy nhất** vừa xử lý mới vừa xử lý replay — tránh code trùng lặp và đảm bảo state luôn nhất quán giữa 2 ngữ cảnh.

**Q5: Tại sao cần `AggregateRepository.save()` — không có nó thì sao?**
A: Không có `save()` → event nằm im trong RAM (`changes`) → **không ghi DB, không publish, saga không chạy, read-model không được tạo**. Transfer "biến mất" khi server restart. `save()` là bước **bắt buộc** để: persist event + chống xung đột + kích hoạt toàn bộ hậu cảnh (Saga/Projection).

**Q6: Tại sao phải truyền `transfer.getVersion()` vào `save()`?**
A: Vì đó là **`expectedVersion`** — dùng cho **optimistic concurrency**. Nó cho `EventStore.append()` biết event đầu nên có version mấy (mới → 0, tiếp theo → 1), và phát hiện xung đột nếu ai đó đã ghi vào cùng stream trước mình. Nếu không truyền (hoặc truyền sai), hệ thống không thể chống **lost update** — 2 thao tác đồng thời có thể ghi đè nhau.

**Q7: Tại sao cần `EventStore.append()` kiểm tra `maxVersion` (optimistic lock)?**
A: Để **đảm bảo tính toàn vẹn của chuỗi events**. Mỗi stream phải có version tăng dần không trùng. Nếu 2 request cùng ghi vào cùng stream, request thứ 2 sẽ thấy version đã đổi (≠ expected) → `ConcurrentModificationException` → bị từ chối, dữ liệu không bị ghi đè/hỏng. Đây là nền tảng để replay an toàn.

**Q8: Tại sao phải `publishAll()` sau khi `append()`?**
A: Vì Event Sourcing tách **"ghi dữ liệu"** và **"phản ứng với dữ liệu"** (side-effects: cập nhật projection, chạy saga, gửi thông báo). Chỉ publish **sau khi** event đã persist (`markRecorded()`) để đảm bảo các listener không bao giờ xử lý dữ liệu chưa bền vững — nếu crash, event vẫn còn trong DB để xử lý lại sau.

**Q9: Tại sao Saga tách riêng debit/credit thành 2 lần `save()` thay vì 1 transaction DB?**
A: Vì `acc-111` và `acc-222` là **2 aggregate/2 stream độc lập** — không thể gói trong 1 `save()` (mỗi `save` chỉ ghi 1 aggregate). Cần điều phối qua **Saga** và dùng **compensation** để khôi phục nếu 1 bước thất bại. Đây là cách xử lý **distributed transaction** trong mô hình microservice/event-driven.

**Q10: Tại sao cần `load()` lại trong Saga bước 3 để gọi `complete()`?**
A: Vì để `complete()` hoạt động đúng, aggregate phải biết trạng thái hiện tại (phải là `PENDING`). Replay lần nữa đảm bảo: nếu server restart giữa chừng, transfer vẫn tái dựng về `PENDING` và saga có thể tiếp tục/kết thúc đúng — trạng thái không bao giờ bị mất.

**Q11: Tại sao cần Projection/read-model khi đã có event store?**
A: Vì **replay toàn bộ events để đọc 1 transfer rất chậm** (phải scan tất cả event). Read-model `transfer_view` được denormalize sẵn → query nhanh. Đây là bản chất **CQRS**: write side tối ưu cho ghi (append events), read side tối ưu cho đọc (bảng query sẵn).

**Q12: Tại sao cần `@Transactional` trên saga?**
A: Để đảm bảo **atomicity** của toàn bộ chuỗi (debit → credit → complete). Nếu bất kỳ bước nào fail, `throw ex` **rollback toàn transaction** → không có trạng thái "lưng chừng" không nhất quán. Kết hợp với compensation để tự sửa sao cho hệ thống nhất quán cuối cùng.

**Q13: Tại sao cần `recorded` flag khi publish?**
A: Để **chỉ publish event đã thực sự được lưu vào DB** (`publishAll` kiểm tra `event.isRecorded()`). Điều này ngăn trường hợp event vừa tạo trong RAM nhưng chưa ghi thành công vẫn bị đẩy đi xử lý → tránh các listener làm việc trên dữ liệu "ma".

**Q14: Tại sao cơ chế "ghi rồi mới publish" lại quan trọng trong transfer?**
A: Vì transfer ảnh hưởng đến **tiền thật** trên 2 tài khoản. Nếu publish trước khi ghi, saga có thể chạy debit/credit dựa trên event chưa lưu → nếu crash, event mất nhưng thao tác đã xảy ra → **mất nhất quán nghiêm trọng**. Đảo thứ tự (ghi trước, publish sau) đảm bảo event bền vững trước khi bất kỳ phản ứng nào diễn ra.

**Q15: Tại sao phải có constructor rỗng trên mỗi event class?**
A: Vì Jackson cần constructor rỗng để **deserialize JSON thành object khi replay**. Nếu thiếu, replay sẽ fail → không tái dựng được trạng thái → toàn bộ hệ thống không khởi động/xử lý được.

---

*Tài liệu được tạo dựa trên việc đọc source code thực tế của project Finaegis Spring Boot.*
