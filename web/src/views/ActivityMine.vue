<template>
  <div class="page">
    <div class="list-head">
      <div class="section-title">
        <span class="bar"></span>我的开团<span class="tip">（团长 / 管理员）</span>
      </div>
      <el-button type="primary" round @click="$router.push('/create-activity')">
        + 发起新团
      </el-button>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" size="default" border>
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待开始</el-radio-button>
        <el-radio-button value="ONGOING">进行中</el-radio-button>
        <el-radio-button value="SUCCESS">已成团</el-radio-button>
        <el-radio-button value="FAILED">已失败</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 活动列表 -->
    <div class="activity-grid" v-if="filteredList.length">
      <el-card
        v-for="a in filteredList"
        :key="a.id"
        class="act-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <span class="act-title">{{ a.title }}</span>
            <el-tag :type="statusTag(a.status)" effect="dark" round size="small">
              {{ statusLabel(a.status) }}
            </el-tag>
          </div>
        </template>

        <div class="price-row">
          <span class="group-price">¥{{ a.groupPrice }}</span>
          <span class="origin-price">¥{{ a.originPrice }}</span>
        </div>

        <div class="stock-row">
          <el-progress
            :percentage="stockPercent(a)"
            :color="progressColor(a)"
            :stroke-width="14"
            :show-text="false"
          />
          <span class="stock-text">
            已参团 <b>{{ joinedQty(a) }}</b> / {{ a.totalStock }} 份
          </span>
        </div>

        <div class="info-row">
          <span>🟢 {{ a.startTime }}</span>
          <span>🔴 {{ a.endTime }}</span>
        </div>

        <div class="card-foot">
          <el-button
            v-if="canChangeTo(a.status, 'ONGOING')"
            size="small"
            type="success"
            round
            :loading="transitingId === a.id"
            @click="changeStatus(a.id, 'ONGOING')"
          >▶ 开始开团</el-button>
          <el-button
            v-if="canChangeTo(a.status, 'SUCCESS')"
            size="small"
            type="warning"
            round
            :loading="transitingId === a.id"
            @click="changeStatus(a.id, 'SUCCESS')"
          >🏁 手动成团</el-button>
          <el-button
            v-if="canChangeTo(a.status, 'FAILED')"
            size="small"
            type="danger"
            round
            :loading="transitingId === a.id"
            @click="changeStatus(a.id, 'FAILED')"
          >✗ 标记失败</el-button>
          <el-button size="small" round @click="$router.push('/home')">📋 去首页</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-else description="还没有创建过开团活动，点右上角发起一个吧 🎯" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const list = ref([])
const statusFilter = ref('')
const transitingId = ref(null)

const filteredList = computed(() =>
  statusFilter.value ? list.value.filter((a) => a.status === statusFilter.value) : list.value
)

function load() {
  request.get('/activities/mine').then((data) => {
    list.value = Array.isArray(data) ? data : []
  }).catch(() => {})
}

function joinedQty(a) {
  if (!a) return 0
  return a.totalStock - (a.stock ?? 0)
}
function stockPercent(a) {
  if (!a || !a.totalStock) return 0
  return Math.min(100, Math.round(((a.totalStock - (a.stock ?? 0)) / a.totalStock) * 100))
}
function progressColor(a) {
  const p = stockPercent(a)
  if (p >= 100) return '#f56c6c'
  if (p >= 70) return '#e6a23c'
  return '#409eff'
}
function statusLabel(s) {
  return { PENDING: '待开始', ONGOING: '进行中', SUCCESS: '已成团', FAILED: '已失败' }[s] ?? s
}
function statusTag(s) {
  return { PENDING: 'info', ONGOING: 'success', SUCCESS: 'warning', FAILED: 'danger' }[s] ?? 'info'
}

// 状态机：允许的流转
const ALLOWED = {
  PENDING: ['ONGOING'],
  ONGOING: ['SUCCESS', 'FAILED']
}
function canChangeTo(from, to) {
  return ALLOWED[from]?.includes(to)
}

function changeStatus(id, target) {
  ElMessageBox.confirm(
    `确定要把活动变更为「${statusLabel(target)}」吗？`,
    '状态变更确认',
    { type: 'warning' }
  ).then(() => {
    transitingId.value = id
    request.put(`/activities/${id}/status?target=${target}`).then(() => {
      ElMessage.success(`已变更为「${statusLabel(target)}」`)
      load()
    }).finally(() => { transitingId.value = null })
  }).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  margin: 16px 0 20px;
}
.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}
.act-card {
  border-radius: 12px;
  transition: transform 0.2s;
}
.act-card:hover {
  transform: translateY(-2px);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.act-title {
  font-weight: 600;
  font-size: 15px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.price-row {
  margin: 8px 0;
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.group-price {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-color-danger, #f56c6c);
}
.origin-price {
  font-size: 13px;
  color: #999;
  text-decoration: line-through;
}
.stock-row {
  margin: 14px 0 10px;
}
.stock-text {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #666;
}
.stock-text b { color: var(--el-color-primary, #409eff); font-size: 15px; }
.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #888;
  margin: 10px 0;
}
.card-foot {
  border-top: 1px dashed #eee;
  padding-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tip {
  color: var(--text-3, #999);
  font-size: 13px;
  font-weight: 400;
  margin-left: 6px;
}
</style>
