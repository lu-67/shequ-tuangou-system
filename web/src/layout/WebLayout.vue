<template>
  <div class="web">
    <!-- 顶部导航 -->
    <header class="w-header">
      <div class="w-header-inner">
        <div class="w-brand" @click="go('/home')">
          <span class="w-logo">🥬</span>
          <div class="w-brand-text">
            <span class="w-name">社区团购</span>
            <span class="w-slogan">产地直供 · 今日下单明日自提</span>
          </div>
        </div>
        <nav class="w-nav">
          <span
            v-for="item in navs"
            :key="item.path"
            :class="{ on: isNav(item.path) }"
            @click="go(item.path)"
          >
            {{ item.label }}
          </span>
        </nav>
        <el-dropdown @command="handleCommand">
          <span class="w-user">
            <el-avatar :size="30" class="w-avatar">{{ avatarText }}</el-avatar>
            <span class="w-uname">{{ user.userInfo?.nickname || user.userInfo?.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="mine">
                <el-icon><User /></el-icon> 个人中心
              </el-dropdown-item>
              <el-dropdown-item v-if="isStaff" command="admin">
                <el-icon><Setting /></el-icon> 进入管理后台
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="w-main">
      <router-view />
    </main>

    <!-- 页脚 -->
    <footer class="w-footer">
      <div class="w-footer-inner">
        <div class="f-brand">
          <span class="f-logo">🥬</span>
          <span>社区团购（生鲜自提）拼团系统 · 居民网页端</span>
        </div>
        <div class="f-note">Java 开发综合实训 · 案例三 · Spring Boot 3 + Vue 3</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessageBox } from 'element-plus'
import { ADMIN_URL } from '../constants'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const navs = [
  { path: '/home', label: '团购首页' },
  { path: '/orders', label: '我的订单' },
  { path: '/mine', label: '个人中心' }
]

const isStaff = computed(
  () => user.userInfo?.role === 'ADMIN' || user.userInfo?.role === 'HEAD'
)
const avatarText = computed(() => {
  const name = user.userInfo?.nickname || user.userInfo?.username || '同'
  return name.charAt(0)
})

function isNav(path) {
  return route.path === path
}

function go(path) {
  if (route.path !== path) router.push(path)
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    // 登录页由 D 同学（M0）实现后改为 router.push('/login')
    ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      .then(() => {
        user.logout()
        router.push('/')
      })
      .catch(() => {})
  } else if (cmd === 'mine') {
    go('/mine')
  } else if (cmd === 'admin') {
    window.open(ADMIN_URL, '_blank')
  }
}
</script>

<style scoped>
.web {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.w-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid var(--border);
  box-shadow: 0 1px 6px rgba(15, 34, 25, 0.05);
}
.w-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 36px;
  padding: 0 24px;
}
.w-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.w-logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #40c057, #2b8a3e);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  box-shadow: 0 2px 6px rgba(47, 158, 68, 0.35);
}
.w-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.w-name {
  font-size: 18px;
  font-weight: 700;
  color: #1e6b2c;
}
.w-slogan {
  font-size: 11px;
  color: var(--text-3);
}
.w-nav {
  display: flex;
  gap: 30px;
  margin-left: auto;
  height: 100%;
}
.w-nav span {
  display: flex;
  align-items: center;
  font-size: 15px;
  color: var(--text-2);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: color 0.15s;
}
.w-nav span:hover {
  color: var(--primary);
}
.w-nav span.on {
  color: var(--primary);
  font-weight: 600;
  border-bottom-color: var(--primary);
}
.w-user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
  font-size: 14px;
  color: var(--text-2);
}
.w-avatar {
  background: var(--primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.w-main {
  flex: 1;
}
.w-footer {
  background: #fff;
  border-top: 1px solid var(--border);
}
.w-footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--text-3);
}
.f-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-2);
}
.f-logo {
  font-size: 16px;
}
</style>
