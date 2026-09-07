# FinAegis Frontend (React + Vite)

Frontend web prototype cho nền tảng **FinAegis Core Banking**, dùng chung cho **cả hai** backend port:
- `finaegis-springboot` (bản **Axon** — Event Sourcing/CQRS/Saga)
- `finaegis-springboot-pure` (bản **viết tay**, không Axon)

Cả hai backend expose cùng một API surface nên **chỉ cần một frontend duy nhất**. Đổi backend nào đang chạy thì chỉ cần đổi base URL.

## Công nghệ

- **React 18** + **Vite 5**
- **Tailwind CSS** 3
- **React Router 6**
- **Axios** (JWT interceptor tự gắn `Authorization: Bearer`)

## Cài đặt

```bash
npm install
```

## Chạy

Đảm bảo một backend (Axon hoặc Pure) đang chạy trên cổng `8080`:

```bash
# Start backend (chọn 1 trong 2)
cd ../finaegis-springboot-pure && mvn spring-boot:run   # bản Pure
# hoặc
cd ../finaegis-springboot && mvn spring-boot:run        # bản Axon

# Start frontend trong thư mục hiện tại
npm run dev
# Mở http://localhost:5173
```

Vite proxy tự động chuyển mọi request `/api/*` tới backend trên cổng `8080` (xem `vite.config.js`).

### Đổi backend sang cổng khác

Sửa 2 chỗ:

1. `vite.config.js` — biến `process.env.VITE_API_TARGET`
2. `.env` — `VITE_API_TARGET`

```bash
# ví dụ backend chạy cổng 9090
VITE_API_TARGET=http://localhost:9090
```

> Nếu backend dùng CORS và bạn muốn gọi thẳng (không qua proxy), đặt:
> `VITE_API_BASE=http://localhost:8080/api` và bỏ proxy trong `vite.config.js`.

## Build production

```bash
npm run build        # kết quả trong dist/
npm run preview      # xem thử bản build
```

## Cấu trúc thư mục

```
frontend/
├── index.html
├── vite.config.js        # dev server + proxy /api -> backend
├── .env                  # VITE_API_TARGET (chọn backend)
├── src/
│   ├── main.jsx          # entry
│   ├── App.jsx           # routes
│   ├── index.css         # Tailwind + components
│   ├── api/
│   │   ├── client.js     # axios instance + JWT interceptor
│   │   └── index.js      # endpoints theo từng domain
│   ├── context/AuthContext.jsx
│   ├── components/
│   │   ├── Layout.jsx        # sidebar + navigation
│   │   ├── ProtectedRoute.jsx
│   │   └── ui.jsx            # Alert, Spinner, badge...
│   └── pages/
│       ├── Login.jsx
│       ├── Register.jsx
│       ├── Dashboard.jsx
│       ├── Accounts.jsx      # create/deposit/withdraw/freeze/close
│       ├── Transfers.jsx     # initiate + lịch sử (saga)
│       ├── Assets.jsx        # danh sách tài sản + convert FX
│       ├── Exchange.jsx      # đặt lệnh matching engine
│       ├── Loans.jsx         # vay + duyệt
│       ├── Stablecoins.jsx   # mint/burn + collateral ratio
│       └── Governance.jsx    # poll + vote + kết quả
```

## Lưu ý

- **Đăng nhập**: tạo tài khoản mới ở trang `/register`, sau đó hệ thống tự đăng nhập.
- **JWT**: token được lưu `localStorage`, tự động gắn vào request; hết hạn (401/403) sẽ tự chuyển về `/login`.
- **Chuyển tiền**: với bản Axon, saga thường chạy bất đồng bộ; hãy kiểm tra lại trạng thái sau vài giây trong màn hình Transfers.
- **FX convert** cần có tỷ giá trong DB; nếu chưa có sẽ báo lỗi "không chuyển đổi được".
