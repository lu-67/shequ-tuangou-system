<template>
  <el-container style="height: 100%">
    <el-aside width="210px" style="background: #001529">
      <div class="logo">社区团购后台</div>
      <!-- B 同学：M2 开团活动页面已实现；其余模块页面待对应组员补充 -->
      <el-menu
        :default-active="activePath"
        background-color="#001529"
        text-color="#a6adb8"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/activities">
          <el-icon><Box /></el-icon>
          <span>开团活动</span>
        </el-menu-item>
        <!-- C 同学：M1 商品管理 -->
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <!-- C 同学：M5 自提核销 -->
        <el-menu-item index="/verification">
          <el-icon><Select /></el-icon>
          <span>自提核销</span>
        </el-menu-item>
      </el-menu>
      <div class="menu-tip">
        <p>待其他模块实现：</p>
        <p>· 登录页 / 用户权限 —— D</p>
        <p>· 统计导出 —— D</p>
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
// 图标由 main.js 全局注册（@element-plus/icons-vue），此处无需单独引入
import { useUserStore } from '../stores/user'

const route = useRoute()
const user = useUserStore()

const pageTitle = computed(() => route.meta.title || '管理后台')
const activePath = computed(() => route.path)

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
