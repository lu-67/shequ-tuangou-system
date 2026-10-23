// 前端用到的常量映射（与后端 schema/data.sql 保持一致）

// 商品分类
export const CATEGORIES = ['蔬菜', '水果', '肉禽', '水产', '蛋奶']

// 分类图标（团购首页卡片封面用）
export const CATEGORY_ICON = {
  蔬菜: '🥬',
  水果: '🍎',
  肉禽: '🍗',
  水产: '🦐',
  蛋奶: '🥛'
}

// 分类封面渐变（卡片封面背景）
export const CATEGORY_GRADIENT = {
  蔬菜: 'linear-gradient(135deg, #e6fcf5 0%, #c3fae8 100%)',
  水果: 'linear-gradient(135deg, #fff4e6 0%, #ffe8cc 100%)',
  肉禽: 'linear-gradient(135deg, #ffe8e8 0%, #ffc9c9 100%)',
  水产: 'linear-gradient(135deg, #e7f5ff 0%, #a5d8ff 100%)',
  蛋奶: 'linear-gradient(135deg, #fff9db 0%, #ffec99 100%)'
}

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

// 订单状态
export const ORDER_STATUS = {
  UNPAID: { text: '待支付', type: 'warning' },
  PAID: { text: '待自提', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' },
  PICKED: { text: '已核销', type: 'info' },
  REFUNDED: { text: '已退款', type: 'danger' }
}

// 管理后台地址（居民端「进入管理后台」按钮跳转用，按实际端口修改）
export const ADMIN_URL = 'http://localhost:5173'
