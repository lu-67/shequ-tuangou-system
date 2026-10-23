import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 居民网页端：端口 5174（管理后台 admin 为 5173，互不冲突）。
// 前端用相对路径 /api 调用，由 Vite 代理转发到后端 8080，避免跨域问题。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
