<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" :loading="exporting" @click="handleExport">
        <el-icon><Download /></el-icon> 导出 Excel
      </el-button>
      <span class="tip">共 {{ orders.length }} 条订单</span>
    </div>

    <el-table :data="orders" border stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" min-width="170" />
      <el-table-column prop="username" label="用户ID" min-width="100" />
      <el-table-column prop="amount" label="支付金额(元)" min-width="130">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" min-width="170" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const orders = ref([])
const loading = ref(false)
const exporting = ref(false)
const user = useUserStore()

const STATUS_MAP = {
  UNPAID: { label: '待支付', type: 'warning' },
  PAID: { label: '已支付', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  PICKED: { label: '已核销', type: 'primary' }
}

function statusLabel(s) {
  return (STATUS_MAP[s] || { label: s || '未知' }).label
}
function statusType(s) {
  return (STATUS_MAP[s] || { type: 'default' }).type
}
function formatMoney(v) {
  const n = Number(v) || 0
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  loading.value = true
  try {
    const data = await request.get('/stats/orders')
    orders.value = Array.isArray(data) ? data : []
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const baseURL = import.meta.env.VITE_API_BASE || '/api'
    const resp = await axios.get(baseURL + '/stats/orders/export', {
      headers: { Authorization: `Bearer ${user.token}` },
      responseType: 'blob'
    })
    const cd = resp.headers['content-disposition'] || ''
    const match = cd.match(/filename\*=utf-8''([^;]+)/) || cd.match(/filename="?([^";]+)"?/)
    const fileName = match ? decodeURIComponent(match[1]) : '社区团购订单数据.xlsx'
    const url = window.URL.createObjectURL(
      new Blob([resp.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败，请确认后端已启动且已登录')
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
.tip { margin-left: 12px; color: #8a94a6; font-size: 13px; }
</style>
