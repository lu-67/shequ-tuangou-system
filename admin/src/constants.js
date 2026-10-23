// 前端用到的常量映射（与后端 schema/data.sql 保持一致）

// 商品分类
export const CATEGORIES = ['蔬菜', '水果', '肉禽', '水产', '蛋奶']

// 商品状态
export const PRODUCT_STATUS = {
  1: { text: '在售', type: 'success' },
  0: { text: '下架', type: 'info' }
}

// 活动状态（状态机）
export const ACTIVITY_STATUS = {
  PENDING: { text: '待开始', type: 'info' },
  ONGOING: { text: '进行中', type: 'warning' },
  SUCCESS: { text: '已成团', type: 'success' },
  FAILED: { text: '已失败', type: 'danger' }
}

// 活动状态可选值（用于状态变更下拉）
export const ACTIVITY_STATUS_OPTIONS = [
  { value: 'PENDING', label: '待开始' },
  { value: 'ONGOING', label: '进行中' },
  { value: 'SUCCESS', label: '已成团' },
  { value: 'FAILED', label: '已失败' }
]

// 订单状态
export const ORDER_STATUS = {
  UNPAID: { text: '待支付', type: 'info' },
  PAID: { text: '已支付', type: 'warning' },
  CANCELLED: { text: '已取消', type: 'info' },
  PICKED: { text: '已核销', type: 'success' },
  REFUNDED: { text: '已退款', type: 'danger' }
}

// 自提点（来自 data.sql，演示数据 id 1~15）
export const PICKUP_POINTS = [
  { id: 1, name: '阳光花园自提点' },
  { id: 2, name: '滨江苑自提点' },
  { id: 3, name: '翠湖天地自提点' },
  { id: 4, name: '锦绣华庭自提点' },
  { id: 5, name: '保利香槟自提点' },
  { id: 6, name: '绿地世纪城自提点' },
  { id: 7, name: '万象城自提点' },
  { id: 8, name: '中海国际自提点' },
  { id: 9, name: '龙湖源著自提点' },
  { id: 10, name: '金科天宸自提点' },
  { id: 11, name: '融创城自提点' },
  { id: 12, name: '恒大绿洲自提点' },
  { id: 13, name: '鲁能星城自提点' },
  { id: 14, name: '协信星澜自提点' },
  { id: 15, name: '金融街自提点' }
]
