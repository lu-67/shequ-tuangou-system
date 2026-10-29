<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>社区团购后台系统登录</h2>
      <el-form :model="form" ref="loginForm" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="handleLogin" :loading="loading">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loginForm = ref(null)
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
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; justify-content: center; align-items: center; background-color: #2c3e50; }
.login-card { width: 400px; padding: 20px; border-radius: 8px; }
h2 { text-align: center; margin-bottom: 20px; color: #333; }
</style>