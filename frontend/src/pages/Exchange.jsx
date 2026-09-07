import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { accountApi, exchangeApi } from '../api'
import { Alert } from '../components/ui'

export default function Exchange() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [form, setForm] = useState({
    accountId: '', symbol: 'BTC/USD', side: 'BUY', type: 'LIMIT',
    quantity: '', price: '',
  })
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)

  const loadAccounts = async () => {
    try {
      const res = await accountApi.list(user.userId)
      setAccounts(res.data)
    } catch {
      /* bỏ qua */
    }
  }

  if (accounts.length === 0) {
    loadAccounts()
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setMessage(null)
    try {
      await exchangeApi.placeOrder({
        accountId: form.accountId,
        symbol: form.symbol,
        side: form.side,
        type: form.type,
        quantity: Number(form.quantity),
        price: form.type === 'MARKET' ? null : Number(form.price),
      })
      setMessage('Đã đặt lệnh vào matching engine.')
    } catch (e) {
      setError(e?.response?.data?.message || 'Đặt lệnh thất bại')
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900 mb-6">Sàn giao dịch (Matching Engine)</h1>
      <div className="max-w-lg">
        <Alert kind="error">{error}</Alert>
        <Alert kind="success">{message}</Alert>
        <div className="card">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label">Tài khoản</label>
              <select className="input" value={form.accountId} required
                onChange={(e) => setForm({ ...form, accountId: e.target.value })}>
                <option value="" disabled>Chọn tài khoản</option>
                {accounts.map((a) => (
                  <option key={a.accountId} value={a.accountId}>{a.name} ({a.assetCode})</option>
                ))}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Cặp tiền</label>
                <select className="input" value={form.symbol}
                  onChange={(e) => setForm({ ...form, symbol: e.target.value })}>
                  {['BTC/USD', 'ETH/USD', 'EUR/USD', 'USD/GBP'].map((s) => <option key={s}>{s}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Loại lệnh</label>
                <select className="input" value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value })}>
                  <option>LIMIT</option>
                  <option>MARKET</option>
                </select>
              </div>
              <div>
                <label className="label">Side</label>
                <select className="input" value={form.side}
                  onChange={(e) => setForm({ ...form, side: e.target.value })}>
                  <option>BUY</option>
                  <option>SELL</option>
                </select>
              </div>
              <div>
                <label className="label">Số lượng</label>
                <input className="input" type="number" step="any" min="0" required
                  value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} />
              </div>
              {form.type === 'LIMIT' && (
                <div className="col-span-2">
                  <label className="label">Giá giới hạn</label>
                  <input className="input" type="number" step="any" min="0" required
                    value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
                </div>
              )}
            </div>
            <button className="btn-primary w-full" type="submit">Đặt lệnh</button>
          </form>
        </div>
      </div>
    </div>
  )
}
