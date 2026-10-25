<template>
  <div>
    <el-card class="page-bar" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="活动标题">
          <el-input v-model="query.title" placeholder="模糊搜索" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="opt in ACTIVITY_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
          <el-button type="success" :icon="Plus" @click="openCreate">发起开团</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="活动标题" min-width="160" />
        <el-table-column prop="productId" label="商品ID" width="80" />
        <el-table-column label="原价/拼团价" width="140">
          <template #default="{ row }">{{ row.originPrice }} / {{ row.groupPrice }}</template>
        </el-table-column>
        <el-table-column label="库存(剩/总)" width="110">
          <template #default="{ row }">{{ row.stock }} / {{ row.totalStock }}</template>
        </el-table-column>
        <el-table-column prop="perLimit" label="限购" width="70" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ACTIVITY_STATUS[row.status]?.type || 'info'">
              {{ ACTIVITY_STATUS[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openStatus(row)">变更状态</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="发起开团活动" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="活动标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="关联商品">
          <el-select v-model="form.productId" style="width: 100%" placeholder="请选择商品">
            <el-option v-for="p in products" :key="p.id" :label="`${p.id}-${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="拼团价">
          <el-input-number v-model="form.groupPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="form.totalStock" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="限购/人">
          <el-input-number v-model="form.perLimit" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="变更活动状态" width="420px">
      <el-form label-width="80px">
        <el-form-item label="当前状态">
          <el-tag :type="ACTIVITY_STATUS[current?.status]?.type || 'info'">
            {{ ACTIVITY_STATUS[current?.status]?.text || current?.status }}
          </el-tag>
        </el-form-item>
        <el-form-item label="目标状态">
          <el-select v-model="targetStatus" style="width: 100%">
            <el-option
              v-for="opt in ACTIVITY_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="changeStatus">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { ACTIVITY_STATUS, ACTIVITY_STATUS_OPTIONS } from '../constants'

const list = ref([])
const products = ref([])
const loading = ref(false)
const query = reactive({ title: '', status: '' })

const createVisible = ref(false)
const saving = ref(false)
const form = reactive({
  title: '',
  productId: null,
  originPrice: 0,
  groupPrice: 0,
  totalStock: 100,
  perLimit: 5,
  startTime: '',
  endTime: ''
})

const statusVisible = ref(false)
const current = ref(null)
const targetStatus = ref('')

function load() {
  loading.value = true
  request
    .get('/activities', { params: query })
    .then((data) => {
      // 后端分页接口返回 { total, pageNum, pageSize, list }，这里做兼容解析
      list.value = Array.isArray(data) ? data : (data && data.list) || []
    })
    .finally(() => {
      loading.value = false
    })
}

function loadProducts() {
  // 商品接口（M1，C 同学负责）尚未实现，静默降级：商品下拉为空不影响活动列表
  request
    .get('/products', { silent: true })
    .then((data) => {
      products.value = Array.isArray(data) ? data : []
    })
    .catch(() => {
      products.value = []
    })
}

function openCreate() {
  Object.assign(form, {
    title: '',
    productId: null,
    originPrice: 0,
    groupPrice: 0,
    totalStock: 100,
    perLimit: 5,
    startTime: '',
    endTime: ''
  })
  createVisible.value = true
}

function create() {
  saving.value = true
  const payload = {
    ...form,
    stock: form.totalStock,
    status: 'PENDING'
  }
  request
    .post('/activities', payload)
    .then(() => {
      ElMessage.success('开团成功')
      createVisible.value = false
      load()
    })
    .catch(() => {
      // 后端 create() 取 SecurityUtils.getUserId()/getRole()，M0 未实现时必然失败
      ElMessage.warning('创建活动需要登录态（M0 用户与权限，D 同学负责），当前后端暂无登录实现')
    })
    .finally(() => {
      saving.value = false
    })
}

function openStatus(row) {
  current.value = row
  targetStatus.value = ''
  statusVisible.value = true
}

function changeStatus() {
  if (!targetStatus.value) {
    ElMessage.warning('请选择目标状态')
    return
  }
  saving.value = true
  // 后端该接口用 @RequestParam 接收 status（不是 JSON body），故拼在 URL 上
  request
    .put(`/activities/${current.value.id}/status?status=${targetStatus.value}`)
    .then(() => {
      ElMessage.success('状态已更新')
      statusVisible.value = false
      load()
    })
    .catch(() => {
      ElMessage.warning('状态变更需要登录态（M0 用户与权限，D 同学负责），当前后端暂无登录实现')
    })
    .finally(() => {
      saving.value = false
    })
}

onMounted(() => {
  load()
  loadProducts()
})
</script>
