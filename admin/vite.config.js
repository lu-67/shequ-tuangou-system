import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器代理：前端用相对路径 /api 调用，由 Vite 转发到后端 8080，
// 从而避免跨域（CORS）问题，也无需在后端写死前端地址。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
