import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { governanceApi } from '../api'
import { Spinner, EmptyState, Alert, statusBadge } from '../components/ui'

export default function Governance() {
  const { user } = useAuth()
  const [polls, setPolls] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({ title: '', description: '', type: 'YES_NO', endDate: '' })
  const [results, setResults] = useState({})

  const load = () => {
    setLoading(true)
    governanceApi
      .polls()
      .then((res) => setPolls(res.data))
      .catch((e) => setError(e?.response?.data?.message || 'Không tải được polls'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const handleCreate = async (e) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await governanceApi.createPoll({
        title: form.title,
        description: form.description,
        type: form.type,
        createdBy: user.userId,
        endDate: form.endDate ? new Date(form.endDate).toISOString() : null,
      })
      setShowCreate(false)
      setForm({ title: '', description: '', type: 'YES_NO', endDate: '' })
      setSuccess('Đã tạo poll.')
      load()
    } catch (e) {
      setError(e?.response?.data?.message || 'Tạo poll thất bại')
    }
  }

  const handleVote = async (pollId, option) => {
    setError(null)
    setSuccess(null)
    try {
      await governanceApi.vote(pollId, { userId: user.userId, option })
      setSuccess('Đã bỏ phiếu.')
      const { data } = await governanceApi.results(pollId)
      setResults((r) => ({ ...r, [pollId]: data }))
    } catch (e) {
      setError(e?.response?.data?.message || 'Bỏ phiếu thất bại')
    }
  }

  const viewResults = async (pollId) => {
    const { data } = await governanceApi.results(pollId)
    setResults((r) => ({ ...r, [pollId]: data }))
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Governance</h1>
        <button className="btn-primary" onClick={() => setShowCreate(true)}>+ Tạo poll</button>
      </div>

      <Alert kind="error">{error}</Alert>
      <Alert kind="success">{success}</Alert>

      {showCreate && (
        <div className="card mb-6">
          <h2 className="font-semibold mb-3">Tạo poll mới</h2>
          <form onSubmit={handleCreate} className="space-y-3">
            <input className="input" placeholder="Tiêu đề" required value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })} />
            <textarea className="input" placeholder="Mô tả" rows={2} value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <select className="input" value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}>
                <option>YES_NO</option>
                <option>SINGLE_CHOICE</option>
                <option>MULTIPLE_CHOICE</option>
              </select>
              <input className="input" type="datetime-local" value={form.endDate}
                onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
            </div>
            <div className="flex gap-2">
              <button type="submit" className="btn-primary">Tạo</button>
              <button type="button" className="btn-secondary" onClick={() => setShowCreate(false)}>Hủy</button>
            </div>
          </form>
        </div>
      )}

      {loading ? <Spinner /> : polls.length === 0 ? <EmptyState text="Chưa có poll nào" /> : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {polls.map((p) => (
            <div key={p.id} className="card">
              <div className="flex justify-between items-start">
                <h3 className="font-semibold text-slate-800">{p.title}</h3>
                <span className={`badge ${statusBadge(p.status)}`}>{p.status}</span>
              </div>
              {p.description && <p className="text-sm text-slate-500 mt-1">{p.description}</p>}
              <div className="text-xs text-slate-400 mt-1">Loại: {p.type}</div>

              <div className="flex gap-2 mt-4">
                <button className="btn-secondary flex-1 text-xs" onClick={() => handleVote(p.id, 'YES')}>Vote YES</button>
                <button className="btn-secondary flex-1 text-xs" onClick={() => handleVote(p.id, 'NO')}>Vote NO</button>
                <button className="btn-secondary text-xs" onClick={() => viewResults(p.id)}>Kết quả</button>
              </div>

              {results[p.id] && (
                <div className="mt-3 bg-slate-50 rounded-lg p-3 text-sm">
                  {Object.entries(results[p.id]).map(([option, count]) => (
                    <div key={option} className="flex justify-between py-0.5">
                      <span>{option}</span>
                      <span className="font-semibold">{count}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
