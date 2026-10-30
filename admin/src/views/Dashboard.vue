<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">订单总数</div>
          <div class="stat-value">{{ overview.totalOrders ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">销售总额（元）</div>
          <div class="stat-value">{{ formatMoney(overview.totalSales) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt" shadow="never">
      <template #header>数据概览说明</template>
      <p class="desc">以上为全平台订单与销售额汇总（不区分订单状态）。</p>
      <p class="desc">可前往「订单管理」查看订单明细，或一键导出 Excel 报表（M6 统计导出）。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const overview = ref({ totalOrders: 0, totalSales: 0 })

function formatMoney(v) {
  const n = Number(v) || 0
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  try {
    const data = await request.get('/api/stats/overview')
    overview.value = data || { totalOrders: 0, totalSales: 0 }
  } catch (e) {
    overview.value = { totalOrders: 0, totalSales: 0 }
  }
}

onMounted(load)
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-title { color: #8a94a6; font-size: 14px; }
.stat-value { color: #303133; font-size: 34px; font-weight: 700; margin-top: 10px; }
.mt { margin-top: 20px; }
.desc { margin: 4px 0; color: #606266; font-size: 14px; }
</style>
