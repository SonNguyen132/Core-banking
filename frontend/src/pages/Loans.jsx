import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { loanApi } from '../api'
import { Spinner, EmptyState, Alert, statusBadge } from '../components/ui'

export default function Loans() {
  const { user } = useAuth()
  const [loans, setLoans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [form, setForm] = useState({ principal: '', interestRate: '10', termMonths: '12', assetCode: 'USD' })
  const [approveLender, setApproveLender] = useState('')

  const load = () => {
    setLoading(true)
    loanApi
      .byBorrower(user.userId)
      .then((res) => setLoans(res.data))
      .catch((e) => setError(e?.response?.data?.message || 'Không tải được danh sách khoản vay'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [user])

  const handleApply = async (e) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await loanApi.apply({
        borrowerId: user.userId,
        principal: Number(form.principal),
        interestRate: Number(form.interestRate),
        termMonths: Number(form.termMonths),
        assetCode: form.assetCode,
      })
      setSuccess('Đã nộp hồ sơ vay.')
      setForm({ ...form, principal: '' })
      load()
    } catch (e) {
      setError(e?.response?.data?.message || 'Nộp hồ sơ thất bại')
    }
  }

  const handleApprove = async (loanId) => {
    if (!approveLender) {
      setError('Nhập lenderId để duyệt khoản vay.')
      return
    }
    try {
      await loanApi.approve(loanId, { lenderId: approveLender })
      setApproveLender('')
      setSuccess('Đã duyệt khoản vay.')
      load()
    } catch (e) {
      setError(e?.response?.data?.message || 'Duyệt thất bại')
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900 mb-6">P2P Lending</h1>

      <Alert kind="error">{error}</Alert>
      <Alert kind="success">{success}</Alert>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="font-semibold mb-4">Vay mới</h2>
          <form onSubmit={handleApply} className="space-y-4">
            <div>
              <label className="label">Gốc vay</label>
              <input className="input" type="number" step="0.01" min="0" required
                value={form.principal} onChange={(e) => setForm({ ...form, principal: e.target.value })} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Lãi suất (%/năm)</label>
                <input className="input" type="number" step="any" min="0" required
                  value={form.interestRate} onChange={(e) => setForm({ ...form, interestRate: e.target.value })} />
              </div>
              <div>
                <label className="label">Kỳ hạn (tháng)</label>
                <input className="input" type="number" min="1" required
                  value={form.termMonths} onChange={(e) => setForm({ ...form, termMonths: e.target.value })} />
              </div>
            </div>
            <div>
              <label className="label">Loại tiền</label>
              <select className="input" value={form.assetCode}
                onChange={(e) => setForm({ ...form, assetCode: e.target.value })}>
                {['USD', 'EUR', 'GBP', 'BTC', 'ETH', 'XAU'].map((c) => <option key={c}>{c}</option>)}
              </select>
            </div>
            <button className="btn-primary w-full" type="submit">Vay</button>
          </form>
        </div>

        <div className="card">
          <h2 className="font-semibold mb-4">Khoản vay của tôi</h2>
          {loading ? <Spinner /> : loans.length === 0 ? <EmptyState text="Chưa có khoản vay" /> : (
            <div className="space-y-2">
              {loans.map((l) => (
                <div key={l.id} className="border rounded-lg p-3 text-sm">
                  <div className="flex justify-between">
                    <span className="font-semibold">{Number(l.principal).toLocaleString()} {l.assetCode}</span>
                    <span className={`badge ${statusBadge(l.status)}`}>{l.status}</span>
                  </div>
                  <div className="text-slate-500 mt-1">
                    {l.interestRate}%/năm · {l.termMonths} tháng · trả {Number(l.monthlyPayment || 0).toLocaleString()}/tháng
                  </div>
                  {l.status === 'PENDING' && (
                    <div className="flex gap-2 mt-2">
                      <input className="input !w-40 !py-1 text-xs" placeholder="lenderId"
                        value={approveLender} onChange={(e) => setApproveLender(e.target.value)} />
                      <button className="btn-secondary text-xs" onClick={() => handleApprove(l.id)}>Duyệt</button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
