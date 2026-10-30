<template>
  <div class="page">
    <div class="list-head">
      <div class="section-title">
        <span class="bar"></span>发起开团<span class="tip">（团长 / 管理员）</span>
      </div>
    </div>

    <!-- 创建开团活动卡片 -->
    <el-card shadow="never" class="create-card">
      <template #header>
        <div class="card-head">
          <span>🆕 新建团购活动</span>
          <el-tag type="warning" effect="dark" round size="small">创建后为「待开始」状态</el-tag>
        </div>
      </template>

      <el-form :model="form" label-width="110px" class="create-form">
        <el-form-item label="活动标题">
          <el-input v-model="form.title" placeholder="如：红富士苹果限时拼团" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="关联商品">
          <el-select v-model="form.productId" style="width: 100%" placeholder="请选择商品" filterable>
            <el-option
              v-for="p in products"
              :key="p.id"
              :label="`${p.name}（${p.category || '未分类'}）`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <div class="row-2">
          <el-form-item label="原价 (¥)">
            <el-input-number v-model="form.originPrice" :min="0.01" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item label="拼团价 (¥)">
            <el-input-number v-model="form.groupPrice" :min="0.01" :precision="2" :step="1" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="总库存">
            <el-input-number v-model="form.totalStock" :min="1" :step="10" />
          </el-form-item>
          <el-form-item label="限购 (件/人)">
            <el-input-number v-model="form.perLimit" :min="1" :max="20" :step="1" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择开始时间"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择结束时间"
            />
          </el-form-item>
        </div>
      </el-form>

      <div class="form-foot">
        <el-button round @click="reset">重置</el-button>
        <el-button type="primary" round :loading="submitting" @click="submit">
          🚀 立即开团
        </el-button>
      </div>
    </el-card>

    <!-- 提示说明 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="开团须知"
      description="1. 创建后活动默认为「待开始」状态，需到管理后台手动变更为「进行中」才能被居民看到；<br>2. 开团价必须低于原价，否则无法成团；<br>3. 活动到期后（结束时间），系统自动判定是否已成团。"
      class="alert"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const products = ref([])
const submitting = ref(false)

const defaultStart = () => {
  const d = new Date()
  d.setMinutes(d.getMinutes() + 30)
  return d.toISOString().slice(0, 19).replace('T', ' ')
}
const defaultEnd = () => {
  const d = new Date()
  d.setDate(d.getDate() + 7)
  d.setHours(23, 59, 0, 0)
  return d.toISOString().slice(0, 19).replace('T', ' ')
}

const form = reactive({
  title: '',
  productId: null,
  originPrice: 0,
  groupPrice: 0,
  totalStock: 100,
  perLimit: 5,
  startTime: defaultStart(),
  endTime: defaultEnd()
})

function loadProducts() {
  request.get('/products/list').then((data) => {
    products.value = Array.isArray(data) ? data : []
    if (products.value.length === 0) {
      ElMessage.warning('暂无可选商品，请联系管理员先上架商品')
    }
  })
}

function reset() {
  Object.assign(form, {
    title: '',
    productId: null,
    originPrice: 0,
    groupPrice: 0,
    totalStock: 100,
    perLimit: 5,
    startTime: defaultStart(),
    endTime: defaultEnd()
  })
}

function submit() {
  // 表单校验
  if (!form.title.trim()) return ElMessage.warning('请填写活动标题')
  if (!form.productId) return ElMessage.warning('请选择关联商品')
  if (form.originPrice <= 0) return ElMessage.warning('原价必须大于0')
  if (form.groupPrice <= 0) return ElMessage.warning('拼团价必须大于0')
  if (form.groupPrice >= form.originPrice) return ElMessage.warning('拼团价必须低于原价')
  if (form.startTime >= form.endTime) return ElMessage.warning('结束时间必须晚于开始时间')

  submitting.value = true
  request
    .post('/activities', { ...form })
    .then(() => {
      ElMessage.success('🎉 开团成功！请到管理后台将活动变更为「进行中」')
      router.push('/home')
    })
    .catch(() => {})
    .finally(() => {
      submitting.value = false
    })
}

onMounted(loadProducts)
</script>

<style scoped>
.create-card {
  border-radius: 14px;
  box-shadow: var(--card-shadow);
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}
.create-form {
  max-width: 720px;
  margin: 0 auto;
}
.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}
.form-foot {
  display: flex;
  justify-content: center;
  gap: 14px;
  padding-top: 18px;
  border-top: 1px dashed #e9ecef;
  margin-top: 8px;
}
.alert {
  margin-top: 18px;
  border-radius: 10px;
}
.tip {
  color: var(--text-3);
  font-size: 13px;
  font-weight: 400;
}
</style>
