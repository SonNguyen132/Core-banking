import { useEffect, useState } from 'react'
import { stablecoinApi } from '../api'
import { Alert } from '../components/ui'

export default function Stablecoins() {
  const [symbols] = useState(['USDC', 'GCU'])
  const [selected, setSelected] = useState('USDC')
  const [supply, setSupply] = useState(null)
  const [ratio, setRatio] = useState(null)
  const [form, setForm] = useState({ mintAmount: '', burnAmount: '', account: 'acc-demo' })
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)

  const load = (symbol) => {
    stablecoinApi.supply(symbol).then((r) => setSupply(r.data)).catch(() => setSupply(null))
    stablecoinApi.collateralRatio(symbol).then((r) => setRatio(r.data)).catch(() => setRatio(null))
  }

  useEffect(() => { load(selected) }, [selected])

  const handleMint = async (e) => {
    e.preventDefault()
    setError(null)
    setMessage(null)
    try {
      const { data } = await stablecoinApi.mint({ symbol: selected, amount: Number(form.mintAmount), targetAccount: form.account })
      setMessage(`Đã mint ${data.amountMinted} ${selected}. Tổng cung mới: ${data.newTotalSupply}`)
      setForm({ ...form, mintAmount: '' })
      load(selected)
    } catch (e) {
      setError(e?.response?.data?.message || 'Mint thất bại')
    }
  }

  const handleBurn = async (e) => {
    e.preventDefault()
    setError(null)
    setMessage(null)
    try {
      const { data } = await stablecoinApi.burn({ symbol: selected, amount: Number(form.burnAmount), targetAccount: form.account })
      setMessage(`Đã burn ${data.amountBurned} ${selected}. Tổng cung mới: ${data.newTotalSupply}`)
      setForm({ ...form, burnAmount: '' })
      load(selected)
    } catch (e) {
      setError(e?.response?.data?.message || 'Burn thất bại')
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900 mb-6">Stablecoin Reserve</h1>

      <div className="flex gap-2 mb-6">
        {symbols.map((s) => (
          <button key={s}
            className={`px-4 py-2 rounded-lg text-sm font-medium ${selected === s ? 'bg-brand-600 text-white' : 'bg-white border border-slate-300'}`}
            onClick={() => setSelected(s)}>
            {s}
          </button>
        ))}
      </div>

      <Alert kind="error">{error}</Alert>
      <Alert kind="success">{message}</Alert>

      {ratio && (
        <div className="card mb-6">
          <h2 className="font-semibold mb-3">{selected} — Collateral Ratio</h2>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <div className="text-sm text-slate-500">Hiện tại</div>
              <div className={`text-xl font-bold ${ratio.underCollateralized ? 'text-red-600' : 'text-green-600'}`}>
                {Number(ratio.currentRatio).toFixed(2)}%
              </div>
            </div>
            <div>
              <div className="text-sm text-slate-500">Tối thiểu</div>
              <div className="text-xl font-bold">{Number(ratio.minRatio).toFixed(2)}%</div>
            </div>
            <div>
              <div className="text-sm text-slate-500">Trạng thái</div>
              <div className={`text-xl font-bold ${ratio.underCollateralized ? 'text-red-600' : 'text-green-600'}`}>
                {ratio.underCollateralized ? 'Thiếu tài sản đảm bảo' : 'An toàn'}
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="font-semibold mb-3">Mint {selected}</h2>
          <div className="text-sm text-slate-500 mb-3">Tổng cung hiện tại: {Number(supply?.totalSupply || 0).toLocaleString()}</div>
          <form onSubmit={handleMint} className="space-y-3">
            <input className="input" type="number" step="any" min="0.01" required placeholder="Số tiền (fiat)"
              value={form.mintAmount} onChange={(e) => setForm({ ...form, mintAmount: e.target.value })} />
            <input className="input" placeholder="Tài khoản nhận (accountId)" value={form.account}
              onChange={(e) => setForm({ ...form, account: e.target.value })} />
            <button className="btn-primary w-full" type="submit">Mint</button>
          </form>
        </div>

        <div className="card">
          <h2 className="font-semibold mb-3">Burn {selected}</h2>
          <div className="text-sm text-slate-500 mb-3">Dự trữ hiện tại: {Number(supply?.reserveAmount || 0).toLocaleString()}</div>
          <form onSubmit={handleBurn} className="space-y-3">
            <input className="input" type="number" step="any" min="0.01" required placeholder="Số token"
              value={form.burnAmount} onChange={(e) => setForm({ ...form, burnAmount: e.target.value })} />
            <input className="input" placeholder="Tài khoản nhận (accountId)" value={form.account}
              onChange={(e) => setForm({ ...form, account: e.target.value })} />
            <button className="btn-danger w-full" type="submit">Burn</button>
          </form>
        </div>
      </div>
    </div>
  )
}
