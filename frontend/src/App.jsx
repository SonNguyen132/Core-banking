import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'

import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import Transfers from './pages/Transfers'
import Assets from './pages/Assets'
import Exchange from './pages/Exchange'
import Loans from './pages/Loans'
import Stablecoins from './pages/Stablecoins'
import Governance from './pages/Governance'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/accounts" element={<Accounts />} />
        <Route path="/transfers" element={<Transfers />} />
        <Route path="/assets" element={<Assets />} />
        <Route path="/exchange" element={<Exchange />} />
        <Route path="/loans" element={<Loans />} />
        <Route path="/stablecoins" element={<Stablecoins />} />
        <Route path="/governance" element={<Governance />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
