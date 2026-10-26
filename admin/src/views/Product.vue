<template>
  <div>
    <el-card class="page-bar" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="商品名称">
          <el-input v-model="query.name" placeholder="模糊搜索" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="在售" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button type="success" :icon="Plus" @click="openCreate">新增商品</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="商品名称" min-width="160" />
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column prop="spec" label="规格" width="110" />
        <el-table-column label="原价 / 团购价" width="150">
          <template #default="{ row }">
            <span class="origin">￥{{ row.price }}</span>
            / <span class="group">￥{{ row.groupPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="PRODUCT_STATUS[row.status]?.type || 'info'">
              {{ PRODUCT_STATUS[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" placeholder="如：有机小白菜" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%" placeholder="请选择分类">
            <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" placeholder="如：500g/份" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="默认：份" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="团购价">
          <el-input-number v-model="form.groupPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.imageUrl" placeholder="URL，可留空" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { CATEGORIES, PRODUCT_STATUS } from '../constants'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ name: '', category: '', status: null, pageNum: 1, pageSize: 10 })

const formVisible = ref(false)
const saving = ref(false)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    id: null,
    name: '',
    category: '蔬菜',
    spec: '',
    unit: '份',
    price: 0,
    groupPrice: 0,
    stock: 100,
    imageUrl: '',
    description: '',
    status: 1
  }
}

function load() {
  loading.value = true
  // 后端分页接口返回 { total, pageNum, pageSize, list }
  request
    .get('/products', { params: query })
    .then((data) => {
      list.value = (data && data.list) || []
      total.value = (data && data.total) || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function search() {
  query.pageNum = 1
  load()
}

function onPageChange(p) {
  query.pageNum = p
  load()
}

function onSizeChange(s) {
  query.pageSize = s
  query.pageNum = 1
  load()
}

function openCreate() {
  Object.assign(form, emptyForm())
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, emptyForm(), row)
  formVisible.value = true
}

function save() {
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请填写商品名称')
    return
  }
  if (Number(form.groupPrice) > Number(form.price)) {
    ElMessage.warning('团购价不能高于原价')
    return
  }
  saving.value = true
  const payload = {
    name: form.name.trim(),
    category: form.category,
    spec: form.spec,
    unit: form.unit,
    price: form.price,
    groupPrice: form.groupPrice,
    stock: form.stock,
    imageUrl: form.imageUrl,
    description: form.description,
    status: form.status
  }
  const req = form.id ? request.put(`/products/${form.id}`, payload) : request.post('/products', payload)
  req
    .then(() => {
      ElMessage.success(form.id ? '商品已更新' : '商品已新增')
      formVisible.value = false
      load()
    })
    .catch(() => {
      // 写接口需要登录态：M0（D 同学）未实现前后端取不到角色，会返回 401
      ElMessage.warning('写操作需要登录态（M0 用户与权限，D 同学负责），当前后端暂无登录实现')
    })
    .finally(() => {
      saving.value = false
    })
}

function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  request
    .put(`/products/${row.id}/status?status=${target}`)
    .then(() => {
      ElMessage.success(target === 1 ? '已上架' : '已下架')
      load()
    })
    .catch(() => {
      ElMessage.warning('写操作需要登录态（M0 用户与权限，D 同学负责），当前后端暂无登录实现')
    })
}

function remove(row) {
  ElMessageBox.confirm(`确认删除商品「${row.name}」？被团购活动引用的商品不可删除。`, '删除确认', {
    type: 'warning'
  })
    .then(() =>
      request.delete(`/products/${row.id}`).then(() => {
        ElMessage.success('已删除')
        load()
      })
    )
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.page-bar {
  margin-bottom: 12px;
}
.page-bar :deep(.el-form-item) {
  margin-bottom: 0;
}
.origin {
  color: #909399;
  text-decoration: line-through;
  font-size: 12px;
}
.group {
  color: #f56c6c;
  font-weight: 600;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
