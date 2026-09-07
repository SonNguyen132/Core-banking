import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { accountApi, transferApi } from '../api'
import { Spinner, EmptyState, Alert, statusBadge } from '../components/ui'

export default function Transfers() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [form, setForm] = useState({ fromAccountId: '', toAccountId: '', amount: '', currency: 'USD', description: '' })
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  useEffect(() => {
    accountApi
      .list(user.userId)
      .then((res) => {
        setAccounts(res.data)
        if (res.data.length) setForm((f) => ({ ...f, fromAccountId: f.fromAccountId || res.data[0].accountId }))
      })
      .catch(() => {})
  }, [user])

  const handleInitiate = async (e) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    setLoading(true)
    try {
      await transferApi.initiate({
        fromAccountId: form.fromAccountId,
        toAccountId: form.toAccountId,
        amount: Number(form.amount),
        currency: form.currency,
        description: form.description,
        initiatedBy: user.userId,
      })
      setSuccess('Đã khởi tạo chuyển tiền thành công (saga sẽ xử lý debit → credit → complete).')
      setForm((f) => ({ ...f, amount: '', toAccountId: '', description: '' }))
    } catch (e) {
      setError(e?.response?.data?.message || 'Chuyển tiền thất bại')
    } finally {
      setLoading(false)
    }
  }

  const loadTransfers = async (accountId) => {
    if (!accountId) return
    setLoading(true)
    setError(null)
    try {
      const res = await transferApi.byAccount(accountId)
      setList(res.data)
    } catch (e) {
      setError(e?.response?.data?.message || 'Không tải được danh sách giao dịch')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900 mb-6">Chuyển tiền</h1>

      <Alert kind="error">{error}</Alert>
      <Alert kind="success">{success}</Alert>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="font-semibold mb-4">Khởi tạo chuyển khoản</h2>
          <form onSubmit={handleInitiate} className="space-y-4">
            <div>
              <label className="label">Từ tài khoản</label>
              <select className="input" value={form.fromAccountId}
                onChange={(e) => setForm({ ...form, fromAccountId: e.target.value })}>
                {accounts.map((a) => (
                  <option key={a.accountId} value={a.accountId}>
                    {a.name} ({a.assetCode}) — {Number(a.balance || 0).toLocaleString()}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Đến tài khoản</label>
              <input className="input" required placeholder="accountId"
                value={form.toAccountId} onChange={(e) => setForm({ ...form, toAccountId: e.target.value })} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Số tiền</label>
                <input className="input" type="number" step="0.01" min="0.01" required
                  value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
              </div>
              <div>
                <label className="label">Loại tiền</label>
                <select className="input" value={form.currency}
                  onChange={(e) => setForm({ ...form, currency: e.target.value })}>
                  {['USD', 'EUR', 'GBP', 'BTC', 'ETH', 'XAU'].map((c) => <option key={c}>{c}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="label">Mô tả</label>
              <input className="input" value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>
            <button type="submit" className="btn-primary w-full" disabled={loading}>
              {loading ? 'Đang xử lý…' : 'Chuyển tiền'}
            </button>
          </form>
        </div>

        <div className="card">
          <h2 className="font-semibold mb-4">Lịch sử giao dịch</h2>
          <div className="mb-3">
            <label className="label">Xem giao dịch của tài khoản</label>
            <select className="input" onChange={(e) => loadTransfers(e.target.value)} defaultValue="">
              <option value="" disabled>Chọn tài khoản</option>
              {accounts.map((a) => (
                <option key={a.accountId} value={a.accountId}>{a.name} ({a.accountId.slice(0, 8)}…)</option>
              ))}
            </select>
          </div>
          {loading ? (
            <Spinner />
          ) : list.length === 0 ? (
            <EmptyState text="Chọn tài khoản để xem lịch sử" />
          ) : (
            <div className="space-y-2">
              {list.map((t) => (
                <div key={t.transferId} className="border rounded-lg p-3 text-sm">
                  <div className="flex justify-between items-center">
                    <span className="font-mono text-xs text-slate-500">{t.transferId.slice(0, 8)}…</span>
                    <span className={`badge ${statusBadge(t.status)}`}>{t.status}</span>
                  </div>
                  <div className="mt-1">
                    {t.fromAccountId.slice(0, 6)}… → {t.toAccountId.slice(0, 6)}…
                  </div>
                  <div className="font-semibold text-brand-600 mt-1">
                    {Number(t.amount).toLocaleString()} {t.currency}
                  </div>
                  {t.description && <div className="text-slate-500 mt-0.5">{t.description}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
