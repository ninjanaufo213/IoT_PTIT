import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Gọi /api/... sẽ được chuyển tới backend Spring (port 8080)
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': 'http://localhost:8080' } },
})
