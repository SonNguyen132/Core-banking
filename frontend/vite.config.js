import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev proxy: mọi request /api được chuyển tới backend.
// Mặc định trỏ 8080 — đổi VITE_API_TARGET để dùng cho Axon hay Pure.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
