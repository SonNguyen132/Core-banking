import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Alert } from '../components/ui'

export default function Login() {
  const { login, loading, error, clearError } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await login(form.email, form.password)
      navigate('/')
    } catch {
      /* error đã set trong context */
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-brand-700 to-brand-900 px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
        <div className="text-center mb-6">
          <div className="text-2xl font-bold text-slate-900">FinAegis</div>
          <p className="text-sm text-slate-500 mt-1">Core Banking Prototype</p>
        </div>
        <h1 className="text-lg font-semibold text-slate-800 mb-4">Đăng nhập</h1>
        <Alert>{error}</Alert>
        <form onSubmit={handleSubmit} className="space-y-4" onClick={clearError}>
          <div>
            <label className="label">Email</label>
            <input
              className="input"
              type="email"
              required
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </div>
          <div>
            <label className="label">Mật khẩu</label>
            <input
              className="input"
              type="password"
              required
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>
          <button className="btn-primary w-full" type="submit" disabled={loading}>
            {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </button>
        </form>
        <p className="text-sm text-center text-slate-500 mt-4">
          Chưa có tài khoản?{' '}
          <Link to="/register" className="text-brand-600 font-medium">
            Đăng ký
          </Link>
        </p>
      </div>
    </div>
  )
}
