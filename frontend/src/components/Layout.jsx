import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const nav = [
  { to: '/', label: 'Dashboard', icon: '🏠' },
  { to: '/accounts', label: 'Accounts', icon: '💳' },
  { to: '/transfers', label: 'Transfers', icon: '📤' },
  { to: '/assets', label: 'Assets & FX', icon: '🌐' },
  { to: '/exchange', label: 'Exchange', icon: '📈' },
  { to: '/loans', label: 'Loans', icon: '🏦' },
  { to: '/stablecoins', label: 'Stablecoins', icon: '🪙' },
  { to: '/governance', label: 'Governance', icon: '🗳️' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex">
      <aside className="w-60 bg-slate-900 text-slate-300 flex flex-col">
        <div className="px-5 py-6 border-b border-slate-800">
          <div className="text-white font-bold text-lg">FinAegis</div>
          <div className="text-xs text-slate-500 mt-0.5">Core Banking Prototype</div>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition ${
                  isActive
                    ? 'bg-brand-600 text-white'
                    : 'hover:bg-slate-800 hover:text-white'
                }`
              }
            >
              <span className="text-base">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="px-5 py-4 border-t border-slate-800">
          <div className="text-sm text-white truncate">{user?.email}</div>
          <button
            onClick={handleLogout}
            className="mt-2 text-xs text-slate-400 hover:text-white"
          >
            Đăng xuất →
          </button>
        </div>
      </aside>
      <main className="flex-1 p-8 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
