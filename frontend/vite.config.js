import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  // Em desenvolvimento, /api vai para o backend local (mesma origem, como em produção).
  server: { proxy: { '/api': 'http://localhost:8080' } },
  test: { environment: 'jsdom', globals: true },
})
