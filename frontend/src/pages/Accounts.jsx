import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { accountApi } from '../api'
import { Spinner, EmptyState, Alert, statusBadge } from '../components/ui'

export default function Accounts() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState({ name: '', assetCode: 'USD' })
  const [action, setAction] = useState(null) // {type:'deposit'|'withdraw', account}
  const [amount, setAmount] = useState('')

  const load = () => {
    setLoading(true)
    accountApi
      .list(user.userId)
      .then((res) => setAccounts(res.data))
      .catch((e) => setError(e?.response?.data?.message || 'Không tải được danh sách'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [user])

  const handleCreate = async (e) => {
    e.preventDefault()
    try {
      await accountApi.create({ name: createForm.name, userId: user.userId, assetCode: createForm.assetCode })
      setShowCreate(false)
      setCreateForm({ name: '', assetCode: 'USD' })
      load()
    } catch (e) {
      setError(e?.response?.data?.message || 'Tạo tài khoản thất bại')
    }
  }

  const handleAction = async (e) => {
    e.preventDefault()
    const { type, account } = action
    try {
      const payload = { amount: Number(amount), currency: account.assetCode, reference: `manual-${type}` }
      if (type === 'deposit') await accountApi.deposit(account.accountId, payload)
      else await accountApi.withdraw(account.accountId, payload)
      setAction(null)
      setAmount('')
      load()
    } catch (e) {
      setError(e?.response?.data?.message || 'Thao tác thất bại')
    }
  }

  const toggleState = async (account, state) => {
    try {
      if (state === 'freeze') await accountApi.freeze(account.accountId)
      else if (state === 'unfreeze') await accountApi.unfreeze(account.accountId)
      else if (state === 'close') await accountApi.close(account.accountId)
      load()
    } catch (e) {
      setError(e?.response?.data?.message || 'Thao tác thất bại')
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Tài khoản</h1>
        <button className="btn-primary" onClick={() => setShowCreate(true)}>
          + Tạo tài khoản
        </button>
      </div>

      <Alert kind="error">{error}</Alert>

      {showCreate && (
        <div className="card mb-6">
          <h2 className="font-semibold mb-3">Tạo tài khoản mới</h2>
          <form onSubmit={handleCreate} className="flex gap-3 items-end">
            <div className="flex-1">
              <label className="label">Tên tài khoản</label>
              <input
                className="input"
                required
                value={createForm.name}
                onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
              />
            </div>
            <div>
              <label className="label">Loại tiền</label>
              <select
                className="input"
                value={createForm.assetCode}
                onChange={(e) => setCreateForm({ ...createForm, assetCode: e.target.value })}
              >
                {['USD', 'EUR', 'GBP', 'BTC', 'ETH', 'XAU'].map((c) => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            </div>
            <button type="submit" className="btn-primary">Tạo</button>
            <button type="button" className="btn-secondary" onClick={() => setShowCreate(false)}>Hủy</button>
          </form>
        </div>
      )}

      {action && (
        <div className="card mb-6">
          <h2 className="font-semibold mb-3">
            {action.type === 'deposit' ? 'Nạp tiền vào' : 'Rút tiền từ'} {action.account.name} (
            {action.account.assetCode})
          </h2>
          <form onSubmit={handleAction} className="flex gap-3 items-end">
            <div>
              <label className="label">Số tiền</label>
              <input
                className="input"
                type="number"
                step="0.01"
                min="0.01"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>
            <button type="submit" className="btn-primary">
              {action.type === 'deposit' ? 'Nạp' : 'Rút'}
            </button>
            <button type="button" className="btn-secondary" onClick={() => setAction(null)}>Hủy</button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? (
          <Spinner />
        ) : accounts.length === 0 ? (
          <EmptyState text="Chưa có tài khoản" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500 border-b">
                  <th className="py-2 pr-4">ID</th>
                  <th className="py-2 pr-4">Tên</th>
                  <th className="py-2 pr-4">Tiền</th>
                  <th className="py-2 pr-4">Số dư</th>
                  <th className="py-2 pr-4">Trạng thái</th>
                  <th className="py-2">Hành động</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((a) => (
                  <tr key={a.accountId} className="border-b last:border-0">
                    <td className="py-2 pr-4 font-mono text-xs text-slate-500">{a.accountId.slice(0, 8)}…</td>
                    <td className="py-2 pr-4 font-medium">{a.name}</td>
                    <td className="py-2 pr-4">{a.assetCode}</td>
                    <td className="py-2 pr-4 font-semibold">{Number(a.balance || 0).toLocaleString()}</td>
                    <td className="py-2 pr-4">
                      <span className={`badge ${statusBadge(a.closed ? 'CANCELLED' : a.frozen ? 'PENDING' : 'ACTIVE')}`}>
                        {a.closed ? 'Closed' : a.frozen ? 'Frozen' : 'Active'}
                      </span>
                    </td>
                    <td className="py-2">
                      <div className="flex gap-1 flex-wrap">
                        <button className="btn-secondary text-xs" disabled={a.closed || a.frozen}
                          onClick={() => setAction({ type: 'deposit', account: a })}>
                          Nạp
                        </button>
                        <button className="btn-secondary text-xs" disabled={a.closed || a.frozen}
                          onClick={() => setAction({ type: 'withdraw', account: a })}>
                          Rút
                        </button>
                        {!a.closed && (
                          a.frozen ? (
                            <button className="btn-secondary text-xs" onClick={() => toggleState(a, 'unfreeze')}>
                              Mở
                            </button>
                          ) : (
                            <button className="btn-secondary text-xs" onClick={() => toggleState(a, 'freeze')}>
                              Đóng băng
                            </button>
                          )
                        )}
                        {!a.closed && (
                          <button className="btn-danger text-xs" onClick={() => toggleState(a, 'close')}>
                            Đóng
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
