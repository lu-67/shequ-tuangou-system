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
import { useUserStore } from '@/stores/user'

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
    // 拦截器在 code===200 时已解包，res 即后端 data：{ token, role, username }
    const data = await request.post('/auth/login', form.value)
    useUserStore().setLogin(data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // 拦截器已统一弹出后端错误信息，这里不再重复提示
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