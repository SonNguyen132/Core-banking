import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { accountApi } from '../api'
import { Spinner, EmptyState, statusBadge } from '../components/ui'

export default function Dashboard() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.userId) return
    accountApi
      .list(user.userId)
      .then((res) => setAccounts(res.data))
      .catch(() => setAccounts([]))
      .finally(() => setLoading(false))
  }, [user])

  const totalBalance = accounts
    .filter((a) => !a.closed)
    .reduce((sum, a) => sum + Number(a.balance || 0), 0)

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900 mb-1">Dashboard</h1>
      <p className="text-slate-500 mb-6">Xin chào {user?.email}</p>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="card">
          <div className="text-sm text-slate-500">Tổng số dư</div>
          <div className="text-2xl font-bold text-brand-600">
            {totalBalance.toLocaleString()} $
          </div>
        </div>
        <div className="card">
          <div className="text-sm text-slate-500">Số tài khoản</div>
          <div className="text-2xl font-bold">{accounts.length}</div>
        </div>
        <div className="card">
          <div className="text-sm text-slate-500">Quick actions</div>
          <div className="flex gap-2 mt-2">
            <Link to="/accounts" className="btn-primary text-xs">+ Account</Link>
            <Link to="/transfers" className="btn-secondary text-xs">Gửi tiền</Link>
          </div>
        </div>
      </div>

      <div className="card">
        <h2 className="font-semibold text-slate-800 mb-4">Tài khoản của bạn</h2>
        {loading ? (
          <Spinner />
        ) : accounts.length === 0 ? (
          <EmptyState text="Bạn chưa có tài khoản nào. Hãy tạo một tài khoản mới." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500 border-b">
                  <th className="py-2 pr-4">Tên</th>
                  <th className="py-2 pr-4">Loại tiền</th>
                  <th className="py-2 pr-4">Số dư</th>
                  <th className="py-2">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((a) => (
                  <tr key={a.accountId} className="border-b last:border-0">
                    <td className="py-2 pr-4">
                      <Link to="/accounts" className="text-brand-600 hover:underline">
                        {a.name}
                      </Link>
                    </td>
                    <td className="py-2 pr-4">{a.assetCode}</td>
                    <td className="py-2 pr-4 font-medium">
                      {Number(a.balance || 0).toLocaleString()}
                    </td>
                    <td className="py-2">
                      <span className={`badge ${statusBadge(a.closed ? 'CANCELLED' : a.frozen ? 'PENDING' : 'ACTIVE')}`}>
                        {a.closed ? 'Đã đóng' : a.frozen ? 'Đóng băng' : 'Hoạt động'}
                      </span>
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
