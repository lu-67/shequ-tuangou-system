<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="logo-title">
        <h2>社区团购 - 居民端</h2>
        <p>新鲜生鲜，产地直发</p>
      </div>
      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名 (如 admin)" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码 (如 123456)" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="success" style="width: 100%" @click="handleLogin" :loading="loading">立即登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
// 注意：如果 web 端的 request 路径不同，请调整下面的导入路径
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const form = ref({
  username: 'admin',
  password: '123456'
})

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入账号密码')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/api/auth/login', form.value)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      ElMessage.success('登录成功')
      // 登录成功后跳转到居民端首页（如果你的首页路由不是 '/' 请自行修改）
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
}
.login-card {
  width: 400px;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.logo-title {
  text-align: center;
  margin-bottom: 25px;
}
h2 {
  color: #2c3e50;
  margin-bottom: 5px;
}
p {
  color: #7f8c8d;
  font-size: 14px;
  margin: 0;
}
</style>