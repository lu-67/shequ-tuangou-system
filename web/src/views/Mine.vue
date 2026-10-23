<template>
  <div class="page">
    <div class="list-head">
      <div class="section-title"><span class="bar"></span>个人中心</div>
    </div>

    <!-- 个人信息 -->
    <section class="profile">
      <div class="p-avatar">
        <el-icon><User /></el-icon>
      </div>
      <div class="p-info">
        <div class="p-name">
          {{ user.userInfo?.nickname || user.userInfo?.username }}
          <el-tag size="small" effect="dark" round class="p-role">{{ roleText }}</el-tag>
        </div>
        <div class="p-sub">账号 {{ user.userInfo?.username }} · 用户ID {{ user.userInfo?.userId }}</div>
      </div>
      <div class="p-actions">
        <el-button v-if="isStaff" type="primary" plain round @click="goAdmin">进入管理后台</el-button>
        <el-button round @click="load">刷新数据</el-button>
      </div>
    </section>

    <!-- 订单统计 -->
    <div class="stats">
      <div class="stat s-blue">
        <div class="s-icon"><el-icon><List /></el-icon></div>
        <div class="s-num">{{ orders.length }}</div>
        <div class="s-label">全部订单</div>
      </div>
      <div class="stat s-orange">
        <div class="s-icon"><el-icon><Wallet /></el-icon></div>
        <div class="s-num">{{ countBy('UNPAID') }}</div>
        <div class="s-label">待支付</div>
      </div>
      <div class="stat s-green">
        <div class="s-icon"><el-icon><Van /></el-icon></div>
        <div class="s-num">{{ countBy('PAID') }}</div>
        <div class="s-label">待自提</div>
      </div>
      <div class="stat s-gray">
        <div class="s-icon"><el-icon><CircleCheck /></el-icon></div>
        <div class="s-num">{{ countBy('PICKED') }}</div>
        <div class="s-label">已核销</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick">
      <el-button type="primary" round @click="$router.push('/home')">去逛逛</el-button>
      <el-button round @click="$router.push('/orders')">查看我的订单</el-button>
    </div>

    <!-- 说明 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="购物流程"
      description="1. 团购首页选择活动参团下单 → 2. 我的订单完成支付 → 3. 支付成功后生成自提核销码 → 4. 凭核销码到所选自提点取货（同一核销码仅可核销一次）。"
      class="alert"
    />
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="温馨提示"
      description="订单需在 30 分钟内完成支付，超时将自动取消并释放库存。"
      class="alert"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import { ADMIN_URL } from '../constants'

const user = useUserStore()
const orders = ref([])

const ROLE_TEXT = { ADMIN: '管理员', HEAD: '团长', RESIDENT: '居民' }
const roleText = computed(() => ROLE_TEXT[user.userInfo?.role] || user.userInfo?.role || '—')
const isStaff = computed(
  () => user.userInfo?.role === 'ADMIN' || user.userInfo?.role === 'HEAD'
)

function countBy(status) {
  return orders.value.filter((o) => o.status === status).length
}

function load() {
  request.get('/orders/my').then((data) => {
    orders.value = data || []
  })
}

function goAdmin() {
  window.open(ADMIN_URL, '_blank')
}

onMounted(load)
</script>

<style scoped>
.profile {
  display: flex;
  align-items: center;
  gap: 20px;
  background: linear-gradient(115deg, #2b8a3e 0%, #40c057 70%);
  border-radius: 16px;
  padding: 28px 32px;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.profile::after {
  content: '';
  position: absolute;
  right: -40px;
  top: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.p-avatar {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 2px solid rgba(255, 255, 255, 0.55);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
  z-index: 1;
}
.p-info {
  flex: 1;
  z-index: 1;
}
.p-name {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.p-role {
  font-weight: 400;
}
.p-sub {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 6px;
}
.p-actions {
  display: flex;
  gap: 10px;
  z-index: 1;
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 18px;
}
.stat {
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: var(--card-shadow);
  display: flex;
  align-items: center;
  gap: 14px;
}
.s-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.s-blue .s-icon {
  background: #e7f5ff;
  color: #1c7ed6;
}
.s-orange .s-icon {
  background: #fff4e6;
  color: #f08c00;
}
.s-green .s-icon {
  background: var(--primary-light);
  color: var(--primary);
}
.s-gray .s-icon {
  background: #f1f3f5;
  color: #868e96;
}
.s-num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-1);
}
.s-label {
  font-size: 13px;
  color: var(--text-3);
  margin-top: 3px;
}
.quick {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}
.alert {
  margin-top: 14px;
  border-radius: 10px;
}
</style>
