import axios from 'axios'

// Trong dev, /api được proxy về backend (xem vite.config.js).
// Nếu muốn gọi thẳng một origin cụ thể, set VITE_API_BASE (vd http://localhost:8080/api).
const baseURL = import.meta.env.VITE_API_BASE || '/api'

const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

// Gắn JWT token vào mọi request nếu có
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('finaegis_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Xử lý lỗi chung + logout khi token hết hạn
api.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      localStorage.removeItem('finaegis_token')
      localStorage.removeItem('finaegis_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export default api
