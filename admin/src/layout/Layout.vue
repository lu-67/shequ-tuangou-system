<template>
  <el-container style="height: 100%">
    <el-aside width="210px" style="background: #001529">
      <div class="logo">社区团购后台</div>
      <!-- 模块菜单由各负责组员实现页面后在此补充（见 router/index.js 顶部说明） -->
      <div class="menu-tip">
        <p>各模块页面建设中：</p>
        <p>· 登录页 / 用户权限 —— D</p>
        <p>· 商品管理 / 自提核销 / 统计 —— C</p>
        <p>· 开团活动 —— B</p>
      </div>
    </el-aside>

    <el-container>
      <el-header
        style="
          background: #fff;
          display: flex;
          align-items: center;
          justify-content: space-between;
          border-bottom: 1px solid #ebeef5;
        "
      >
        <span style="font-size: 16px; font-weight: 600">{{ pageTitle }}</span>
        <el-dropdown @command="handleCommand">
          <span class="user-area">
            <el-icon><User /></el-icon>
            <span style="margin-left: 6px">{{ user.userInfo?.nickname || user.userInfo?.username || '未登录' }}</span>
            <el-icon style="margin-left: 4px"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const user = useUserStore()

const pageTitle = computed(() => route.meta.title || '管理后台')

// 登录页由 D 同学实现后，改为 router.push('/login')
function handleCommand(cmd) {
  if (cmd === 'logout') {
    user.logout()
  }
}
</script>

<style scoped>
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
  background: #00284d;
}
.menu-tip {
  padding: 16px;
  color: #8a94a6;
  font-size: 12px;
  line-height: 1.9;
}
.menu-tip p {
  margin: 0;
}
.user-area {
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}
</style>
