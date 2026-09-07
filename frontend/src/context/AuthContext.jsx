import { createContext, useContext, useState, useEffect } from 'react'
import { authApi } from '../api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('finaegis_user') || 'null')
    } catch {
      return null
    }
  })
  const [token, setToken] = useState(() => localStorage.getItem('finaegis_token'))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const persist = (nextUser, nextToken) => {
    setUser(nextUser)
    setToken(nextToken)
    if (nextToken) localStorage.setItem('finaegis_token', nextToken)
    else localStorage.removeItem('finaegis_token')
    if (nextUser) localStorage.setItem('finaegis_user', JSON.stringify(nextUser))
    else localStorage.removeItem('finaegis_user')
  }

  const login = async (email, password) => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await authApi.login({ email, password })
      persist({ email, userId: data.userId }, data.accessToken)
      return data
    } catch (e) {
      setError(extractMessage(e))
      throw e
    } finally {
      setLoading(false)
    }
  }

  const register = async (name, email, password) => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await authApi.register({ name, email, password })
      persist({ email, name, userId: data.userId }, data.accessToken)
      return data
    } catch (e) {
      setError(extractMessage(e))
      throw e
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    persist(null, null)
  }

  const value = { user, token, loading, error, login, register, logout, clearError: () => setError(null) }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

function extractMessage(e) {
  return e?.response?.data?.message || e?.message || 'Đã có lỗi xảy ra'
}

export function useAuth() {
  return useContext(AuthContext)
}
