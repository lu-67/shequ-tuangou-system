<template>
  <div class="page">
    <div class="list-head">
      <div class="section-title">
        <span class="bar"></span>我的订单<span class="count">共 {{ list.length }} 单</span>
      </div>
      <el-segmented v-model="statusFilter" :options="segOptions" @change="load" />
    </div>

    <div v-loading="loading">
      <div v-if="!loading && list.length === 0" class="empty">
        <el-empty description="暂无订单，去首页看看正在进行的拼团吧">
          <el-button type="primary" round @click="$router.push('/home')">去逛逛</el-button>
        </el-empty>
      </div>

      <article v-for="o in list" :key="o.id" class="order-card" :class="'st-' + o.status">
        <div class="o-head">
          <span class="o-no">订单号 {{ o.orderNo }}</span>
          <span class="o-time">下单时间 {{ o.createTime }}</span>
          <el-tag :type="ORDER_STATUS[o.status]?.type || 'info'" effect="light" round size="small">
            {{ ORDER_STATUS[o.status]?.text || o.status }}
          </el-tag>
        </div>
        <div class="o-body">
          <div class="o-cell">
            <span class="k">拼团活动</span>
            <span class="v ellipsis" :title="activityTitle(o)">{{ activityTitle(o) }}</span>
          </div>
          <div class="o-cell">
            <span class="k">购买数量</span>
            <span class="v">{{ o.quantity }} 件</span>
          </div>
          <div class="o-cell">
            <span class="k">订单金额</span>
            <span class="v amt">¥{{ o.amount }}</span>
          </div>
          <div class="o-cell">
            <span class="k">自提点</span>
            <span class="v ellipsis" :title="pointName(o)">{{ pointName(o) }}</span>
          </div>
        </div>

        <!-- 待支付 -->
        <div v-if="o.status === 'UNPAID'" class="o-foot">
          <span class="tip">请在 30 分钟内完成支付，超时订单将自动取消</span>
          <el-button type="danger" round :loading="payingId === o.id" @click="pay(o)">立即支付</el-button>
        </div>

        <!-- 待自提 -->
        <div v-else-if="o.status === 'PAID'" class="o-foot">
          <div class="code-box">
            <span class="k">自提核销码</span>
            <span class="code">{{ o.verificationCode || '—' }}</span>
            <span class="tip">凭该码到自提点由工作人员核销取货</span>
          </div>
          <el-button round plain @click="copyCode(o)">复制核销码</el-button>
        </div>

        <div v-else-if="o.status === 'PICKED'" class="o-foot">
          <span class="tip">已于自提点完成核销，感谢参与</span>
          <el-button round plain disabled>已完成</el-button>
        </div>

        <div v-else-if="o.status === 'CANCELLED'" class="o-foot">
          <span class="tip">订单已取消（超时未支付或手动取消）</span>
        </div>

        <div v-else-if="o.status === 'REFUNDED'" class="o-foot">
          <span class="tip">订单已退款</span>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { ORDER_STATUS } from '../constants'

const list = ref([])
const activities = ref([])
const points = ref([])
const loading = ref(false)
const payingId = ref(null)
const statusFilter = ref('')

const segOptions = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'UNPAID' },
  { label: '待自提', value: 'PAID' },
  { label: '已核销', value: 'PICKED' }
]

function activityTitle(o) {
  const a = activities.value.find((x) => x.id === o.activityId)
  return a ? a.title : `活动 #${o.activityId}`
}

function pointName(o) {
  const p = points.value.find((x) => x.id === o.pickupPointId)
  return p ? `${p.name}（${p.address}）` : `自提点 #${o.pickupPointId}`
}

function load() {
  loading.value = true
  Promise.all([
    request.get('/orders/my'),
    request.get('/activities/ongoing'),
    request.get('/pickup/points')
  ])
    .then(([orders, acts, pts]) => {
      activities.value = acts || []
      points.value = pts || []
      let arr = orders || []
      if (statusFilter.value) {
        arr = arr.filter((o) => o.status === statusFilter.value)
      }
      list.value = arr
    })
    .finally(() => {
      loading.value = false
    })
}

function pay(o) {
  payingId.value = o.id
  request
    .post(`/payments/pay/${o.id}`)
    .then(() => {
      ElMessage.success('支付成功，请到自提点出示核销码')
      load()
    })
    .catch(() => {})
    .finally(() => {
      payingId.value = null
    })
}

function copyCode(o) {
  const code = o.verificationCode || ''
  if (!code) {
    ElMessage.warning('暂无核销码')
    return
  }
  if (navigator.clipboard) {
    navigator.clipboard.writeText(code)
    ElMessage.success(`已复制核销码 ${code}`)
  } else {
    ElMessage.info(`核销码：${code}`)
  }
}

onMounted(load)
</script>

<style scoped>
.order-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 22px;
  margin-bottom: 16px;
  box-shadow: var(--card-shadow);
  border-left: 4px solid var(--border);
  transition: box-shadow 0.2s;
}
.order-card:hover {
  box-shadow: var(--card-shadow-hover);
}
.order-card.st-UNPAID {
  border-left-color: #f08c00;
}
.order-card.st-PAID {
  border-left-color: var(--primary);
}
.order-card.st-PICKED {
  border-left-color: #adb5bd;
}
.order-card.st-CANCELLED,
.order-card.st-REFUNDED {
  border-left-color: #e03131;
}
.o-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f3f5;
}
.o-no {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}
.o-time {
  font-size: 12px;
  color: var(--text-3);
  margin-right: auto;
}
.o-body {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px 20px;
  padding: 16px 0;
}
@media (max-width: 720px) {
  .o-body {
    grid-template-columns: repeat(2, 1fr);
  }
}
.o-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 13px;
  min-width: 0;
}
.k {
  color: var(--text-3);
  font-size: 12px;
}
.v {
  color: var(--text-1);
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.amt {
  color: var(--price);
  font-weight: 700;
  font-size: 16px;
}
.o-foot {
  border-top: 1px dashed #f1f3f5;
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.tip {
  font-size: 12px;
  color: var(--text-3);
}
.code-box {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.code {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 3px;
  color: var(--primary-dark);
  background: var(--primary-light);
  border: 1px dashed var(--primary);
  padding: 5px 14px;
  border-radius: 8px;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
}
.empty {
  padding: 64px 0;
  background: #fff;
  border-radius: 14px;
}
</style>
