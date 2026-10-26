<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" header="自提核销">
          <el-form :model="form" label-width="90px">
            <el-form-item label="核销码">
              <el-input
                v-model="form.verificationCode"
                placeholder="请输入订单核销码，如 CG10000001"
                clearable
                size="large"
                @keyup.enter="submit"
              />
            </el-form-item>
            <el-form-item label="自提点">
              <el-select v-model="form.pickupPointId" placeholder="请选择自提点" style="width: 100%">
                <el-option
                  v-for="p in points"
                  :key="p.id"
                  :label="`${p.name}（${p.address}）`"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" @click="submit">
                确认核销
              </el-button>
              <el-button size="large" @click="reset">清空</el-button>
            </el-form-item>
          </el-form>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="同一核销码连续核销只会成功一次：第 2 次起返回「该订单已核销，不可重复核销」（错误码 4002）。"
          />
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" header="本次核销结果">
          <el-empty v-if="!lastResult" description="暂无核销记录" :image-size="80" />
          <el-descriptions v-else :column="1" border size="small">
            <el-descriptions-item label="结果">
              <el-tag :type="lastResult.success ? 'success' : 'danger'">
                {{ lastResult.success ? '核销成功' : '核销失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="核销码">{{ lastResult.code }}</el-descriptions-item>
            <el-descriptions-item label="自提点">{{ lastResult.pointName }}</el-descriptions-item>
            <el-descriptions-item label="说明">{{ lastResult.message }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ lastResult.time }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const points = ref([])
const submitting = ref(false)
const lastResult = ref(null)

const form = reactive({ verificationCode: '', pickupPointId: null })

function loadPoints() {
  // 自提点接口（M5）已实现，返回数组
  request
    .get('/pickup/points')
    .then((data) => {
      points.value = Array.isArray(data) ? data : []
    })
    .catch(() => {
      points.value = []
    })
}

function now() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function submit() {
  if (!form.verificationCode.trim()) {
    ElMessage.warning('请输入核销码')
    return
  }
  if (!form.pickupPointId) {
    ElMessage.warning('请选择自提点')
    return
  }
  const point = points.value.find((p) => p.id === form.pickupPointId)
  submitting.value = true
  request
    .post('/pickup/verify', {
      verificationCode: form.verificationCode.trim(),
      pickupPointId: form.pickupPointId
    })
    .then((id) => {
      lastResult.value = {
        success: true,
        code: form.verificationCode.trim(),
        pointName: point ? point.name : `#${form.pickupPointId}`,
        message: `核销记录 ID ${id}`,
        time: now()
      }
      ElMessage.success('核销成功')
      form.verificationCode = ''
    })
    .catch((e) => {
      // 后端业务异常（4001 核销码无效 / 4002 重复核销 / 3002 未支付）都走这里
      const msg = (e && e.message) || '核销失败'
      lastResult.value = {
        success: false,
        code: form.verificationCode.trim(),
        pointName: point ? point.name : `#${form.pickupPointId}`,
        message: msg,
        time: now()
      }
    })
    .finally(() => {
      submitting.value = false
    })
}

function reset() {
  form.verificationCode = ''
  form.pickupPointId = null
  lastResult.value = null
}

onMounted(loadPoints)
</script>
