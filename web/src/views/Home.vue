<template>
  <div class="page">
    <!-- 顶部横幅 -->
    <section class="hero">
      <div class="hero-text">
        <div class="hero-badge">🌱 今日新鲜开团</div>
        <h1>产地直供 · 社区拼团</h1>
        <p>今日下单，次日到自提点取货；人多价更优，坏果包赔。</p>
        <div class="hero-tags">
          <span>🚚 次日自提</span>
          <span>✅ 坏果包赔</span>
          <span>💰 团长价</span>
        </div>
      </div>
      <div class="hero-emoji">🛒</div>
    </section>

    <!-- 列表头部 -->
    <div class="list-head">
      <div class="section-title">
        <span class="bar"></span>全部拼团<span class="count">{{ visibleList.length }} 场活动</span>
      </div>
      <div class="list-tools">
        <el-input
          v-model="keyword"
          placeholder="搜索拼团活动 / 商品"
          clearable
          style="width: 240px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="cats">
          <div class="cat-chip" :class="{ on: cat === '' }" @click="cat = ''">全部</div>
          <div
            v-for="c in CATEGORIES"
            :key="c"
            class="cat-chip"
            :class="{ on: cat === c }"
            @click="cat = c"
          >
            {{ CATEGORY_ICON[c] || '🛒' }} {{ c }}
          </div>
        </div>
      </div>
    </div>

    <!-- 活动卡片网格 -->
    <div v-loading="loading">
      <div v-if="!loading && visibleList.length === 0" class="empty">
        <el-empty description="暂无拼团活动" />
      </div>
      <div v-else class="grid">
        <article v-for="a in visibleList" :key="a.id" class="act-card">
          <div class="card-cover" :style="{ background: coverBg(a) }">
            <span class="cover-emoji">{{ categoryIcon(a) }}</span>
            <span class="cover-cat">{{ categoryOf(a) || '生鲜' }}</span>
            <el-tag
              :type="ACTIVITY_STATUS[a.status]?.type || 'info'"
              size="small"
              effect="dark"
              class="card-tag"
            >
              {{ ACTIVITY_STATUS[a.status]?.text || a.status }}
            </el-tag>
          </div>
          <div class="card-body">
            <h3 class="card-title" :title="a.title">{{ a.title }}</h3>
            <div class="card-sub">{{ productName(a) || '产地生鲜好物' }}</div>
            <div class="card-price">
              <span class="cur">¥</span>
              <span class="group">{{ a.groupPrice }}</span>
              <span class="origin">¥{{ a.originPrice }}</span>
            </div>
            <div class="card-progress">
              <el-progress
                :percentage="soldPercent(a)"
                :stroke-width="8"
                color="#e03131"
                :show-text="false"
              />
              <span class="sold">已抢 {{ a.totalStock - a.stock }} / {{ a.totalStock }}</span>
            </div>
            <div class="card-foot">
              <span class="limit">限购 {{ a.perLimit }} 件</span>
              <div class="card-btns">
                <el-button size="small" round plain @click="openDetail(a)">详情</el-button>
                <el-button
                  size="small"
                  round
                  type="danger"
                  :disabled="a.status !== 'ONGOING' || a.stock <= 0"
                  @click="openBuy(a)"
                >
                  立即参团
                </el-button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="活动详情" width="580px">
      <div v-if="current">
        <div class="d-hero" :style="{ background: coverBg(current) }">
          <span class="d-emoji">{{ categoryIcon(current) }}</span>
        </div>
        <h3 class="d-title">{{ current.title }}</h3>
        <div class="d-price">
          <span class="cur">¥</span>
          <span class="group">{{ current.groupPrice }}</span>
          <span class="origin">原价 ¥{{ current.originPrice }}</span>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="关联商品">{{ productName(current) || '—' }}</el-descriptions-item>
          <el-descriptions-item label="商品分类">{{ categoryOf(current) || '—' }}</el-descriptions-item>
          <el-descriptions-item label="剩余库存">{{ current.stock }} / {{ current.totalStock }}</el-descriptions-item>
          <el-descriptions-item label="每人限购">{{ current.perLimit }} 件</el-descriptions-item>
          <el-descriptions-item label="活动状态">
            {{ ACTIVITY_STATUS[current.status]?.text || current.status }}
          </el-descriptions-item>
          <el-descriptions-item label="自提方式">到指定自提点核销取货</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ current.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ current.endTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button round @click="detailVisible = false">关闭</el-button>
        <el-button
          round
          type="danger"
          :disabled="!current || current.status !== 'ONGOING' || current.stock <= 0"
          @click="openBuy(current)"
        >
          我要参团
        </el-button>
      </template>
    </el-dialog>

    <!-- 参团下单 -->
    <el-dialog v-model="buyVisible" title="确认参团" width="460px">
      <el-form v-if="current" label-width="90px">
        <el-form-item label="拼团活动">
          <span>{{ current.title }}</span>
        </el-form-item>
        <el-form-item label="拼团价">
          <span class="buy-price">¥{{ current.groupPrice }}</span>
        </el-form-item>
        <el-form-item label="购买数量">
          <el-input-number
            v-model="buyQty"
            :min="1"
            :max="Math.min(current.perLimit, current.stock)"
          />
          <span style="margin-left: 8px; color: #999">（限购 {{ current.perLimit }} 件）</span>
        </el-form-item>
        <el-form-item label="自提点">
          <el-select v-model="buyPointId" placeholder="请选择自提点" style="width: 100%">
            <el-option
              v-for="p in points"
              :key="p.id"
              :label="`${p.name}（${p.address}）`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="buy-price">¥{{ (current.groupPrice * buyQty).toFixed(2) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="buyVisible = false">取消</el-button>
        <el-button round type="danger" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { CATEGORIES, ACTIVITY_STATUS, CATEGORY_ICON, CATEGORY_GRADIENT } from '../constants'

const router = useRouter()
const list = ref([])
const points = ref([])
const loading = ref(false)
const keyword = ref('')
const cat = ref('')
const current = ref(null)
const detailVisible = ref(false)
const buyVisible = ref(false)
const buyQty = ref(1)
const buyPointId = ref(null)
const submitting = ref(false)

// 商品ID -> 商品，用于分类筛选与展示
const products = ref([])
const productMap = computed(() => {
  const m = {}
  for (const p of products.value) m[p.id] = p
  return m
})

const visibleList = computed(() => {
  let arr = list.value
  if (keyword.value) {
    const k = keyword.value.trim()
    arr = arr.filter(
      (a) => (a.title && a.title.includes(k)) || (productName(a) && productName(a).includes(k))
    )
  }
  if (cat.value) {
    const ids = products.value.filter((p) => p.category === cat.value).map((p) => p.id)
    arr = arr.filter((a) => ids.includes(a.productId))
  }
  return arr
})

function productName(a) {
  const p = productMap.value[a.productId]
  return p ? p.name : ''
}

function categoryOf(a) {
  const p = productMap.value[a.productId]
  return p ? p.category : ''
}

function categoryIcon(a) {
  return CATEGORY_ICON[categoryOf(a)] || '🥬'
}

function coverBg(a) {
  return CATEGORY_GRADIENT[categoryOf(a)] || 'linear-gradient(135deg, #e6fcf5 0%, #c3fae8 100%)'
}

function soldPercent(a) {
  if (!a.totalStock) return 0
  const sold = a.totalStock - a.stock
  return Math.round((sold / a.totalStock) * 100)
}

function load() {
  loading.value = true
  // 三个接口均返回数组：进行中活动、上架商品、自提点
  Promise.all([request.get('/activities/ongoing'), request.get('/products/list'), request.get('/pickup/points')])
    .then(([acts, prods, pts]) => {
      list.value = acts || []
      products.value = prods || []
      points.value = pts || []
    })
    .finally(() => {
      loading.value = false
    })
}

function openDetail(a) {
  current.value = a
  detailVisible.value = true
}

function openBuy(a) {
  current.value = a
  buyQty.value = 1
  buyPointId.value = points.value.length ? points.value[0].id : null
  detailVisible.value = false
  buyVisible.value = true
}

function submitOrder() {
  if (!buyPointId.value) {
    ElMessage.warning('请选择自提点')
    return
  }
  submitting.value = true
  request
    .post('/orders', {
      activityId: current.value.id,
      quantity: buyQty.value,
      pickupPointId: buyPointId.value
    })
    .then((orderId) => {
      ElMessage.success(`下单成功，订单ID ${orderId}，请尽快完成支付`)
      buyVisible.value = false
      router.push('/orders')
    })
    .catch(() => {})
    .finally(() => {
      submitting.value = false
    })
}

onMounted(load)
</script>

<style scoped>
/* 横幅 */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(115deg, #2b8a3e 0%, #40c057 55%, #66c164 100%);
  border-radius: 16px;
  padding: 38px 44px;
  color: #fff;
  margin-bottom: 22px;
  position: relative;
  overflow: hidden;
}
.hero::after {
  content: '';
  position: absolute;
  right: -60px;
  top: -60px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.hero::before {
  content: '';
  position: absolute;
  right: 90px;
  bottom: -90px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}
.hero-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  margin-bottom: 14px;
}
.hero-text h1 {
  margin: 0 0 10px;
  font-size: 28px;
  letter-spacing: 1px;
}
.hero-text p {
  margin: 0;
  font-size: 14px;
  opacity: 0.92;
}
.hero-tags {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}
.hero-tags span {
  background: rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  padding: 5px 12px;
  font-size: 12px;
}
.hero-emoji {
  font-size: 76px;
  z-index: 1;
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, 0.18));
}

/* 卡片网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
@media (max-width: 992px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
.act-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
}
.act-card:hover {
  box-shadow: var(--card-shadow-hover);
  transform: translateY(-3px);
}
.card-cover {
  position: relative;
  height: 132px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover-emoji {
  font-size: 56px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.08));
}
.cover-cat {
  position: absolute;
  left: 12px;
  bottom: 12px;
  background: rgba(255, 255, 255, 0.85);
  color: var(--text-2);
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}
.card-tag {
  position: absolute;
  top: 12px;
  right: 12px;
}
.card-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-sub {
  font-size: 12px;
  color: var(--text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.cur {
  color: var(--price);
  font-size: 14px;
  font-weight: 700;
}
.group {
  color: var(--price);
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}
.origin {
  color: #c0c4cc;
  text-decoration: line-through;
  font-size: 12px;
}
.card-progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sold {
  font-size: 12px;
  color: var(--text-3);
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 4px;
}
.limit {
  font-size: 12px;
  color: var(--text-3);
}
.card-btns {
  display: flex;
  gap: 8px;
}
.empty {
  padding: 48px 0;
  background: #fff;
  border-radius: 14px;
}

/* 详情弹窗 */
.d-hero {
  height: 120px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.d-emoji {
  font-size: 52px;
}
.d-title {
  margin: 0 0 8px;
  font-size: 18px;
}
.d-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 16px;
}
.d-price .group {
  font-size: 26px;
}
.d-price .origin {
  font-size: 12px;
}
.buy-price {
  color: var(--price);
  font-weight: 700;
  font-size: 16px;
}
</style>
