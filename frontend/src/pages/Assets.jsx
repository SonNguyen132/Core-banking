import { useEffect, useState } from 'react'
import { assetApi } from '../api'
import { Spinner, EmptyState, Alert } from '../components/ui'

export default function Assets() {
  const [assets, setAssets] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ from: '', to: '', amount: '' })
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    assetApi
      .list()
      .then((res) => setAssets(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const handleConvert = async (e) => {
    e.preventDefault()
    setError(null)
    setResult(null)
    try {
      const { data } = await assetApi.convert({
        from: form.from,
        to: form.to,
        amount: Number(form.amount),
      })
      setResult(data)
    } catch (e) {
      setError(e?.response?.data?.message || 'Không chuyển đổi được (thiếu tỷ giá?)')
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900 mb-6">Assets & FX</h1>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="font-semibold mb-4">Danh sách tài sản</h2>
          {loading ? <Spinner /> : assets.length === 0 ? <EmptyState /> : (
            <div className="grid grid-cols-2 gap-3">
              {assets.map((a) => (
                <div key={a.code} className="border rounded-lg p-3">
                  <div className="font-semibold">{a.code}</div>
                  <div className="text-sm text-slate-500">{a.name}</div>
                  <div className="text-xs text-slate-400 mt-1">{a.type} · {a.precision} decimals</div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <h2 className="font-semibold mb-4">Chuyển đổi ngoại tệ</h2>
          <Alert kind="error">{error}</Alert>
          <form onSubmit={handleConvert} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Từ</label>
                <select className="input" value={form.from} required
                  onChange={(e) => setForm({ ...form, from: e.target.value })}>
                  <option value="" disabled>Chọn</option>
                  {assets.map((a) => <option key={a.code}>{a.code}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Sang</label>
                <select className="input" value={form.to} required
                  onChange={(e) => setForm({ ...form, to: e.target.value })}>
                  <option value="" disabled>Chọn</option>
                  {assets.map((a) => <option key={a.code}>{a.code}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="label">Số tiền</label>
              <input className="input" type="number" step="any" min="0" required
                value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
            </div>
            <button className="btn-primary w-full" type="submit">Chuyển đổi</button>
          </form>
          {result && (
            <div className="mt-4 bg-slate-50 border rounded-lg p-4 text-sm">
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Nhận</span>
                <span className="font-semibold">{Number(result.amount).toLocaleString()} {result.from}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Đổi được</span>
                <span className="font-semibold text-brand-600">{Number(result.converted).toLocaleString()} {result.to}</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
