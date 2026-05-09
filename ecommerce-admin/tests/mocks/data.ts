// Mock data for all API responses used in UI tests

export const MOCK_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.mock-admin-token'

export const MOCK_PRODUCTS = [
  {
    id: 1, name: 'iPhone 15', categoryId: 1, brandId: null,
    description: 'Apple 最新款手机', mainImage: 'iphone15.jpg',
    images: '["img1.jpg","img2.jpg"]', detail: '<p>详情内容</p>',
    status: 1, avgRating: 4.8, reviewCount: 256, createdAt: '2026-05-01T10:00:00'
  },
  {
    id: 2, name: 'AirPods Pro', categoryId: 1, brandId: null,
    description: '降噪耳机', mainImage: '',
    images: '', detail: '',
    status: 0, avgRating: null, reviewCount: 0, createdAt: '2026-05-02T10:00:00'
  },
  {
    id: 3, name: 'MacBook Pro', categoryId: 2, brandId: null,
    description: '', mainImage: 'macbook.jpg',
    images: '["mb1.jpg"]', detail: '<p>M4 芯片</p>',
    status: 1, avgRating: 4.9, reviewCount: 89, createdAt: '2026-04-15T08:30:00'
  },
]

export const MOCK_PRODUCT_DETAIL = {
  spu: {
    id: 1, name: 'iPhone 15', categoryId: 1, brandId: null,
    description: 'Apple 最新款手机', mainImage: 'iphone15.jpg',
    images: '["img1.jpg","img2.jpg"]', detail: '<p>详情内容</p>',
    status: 1, avgRating: 4.8, reviewCount: 256, createdAt: '2026-05-01T10:00:00'
  },
  skus: [
    {
      id: 101, spuId: 1, name: '128GB 黑色', spec: '{"color":"黑色","storage":"128GB"}',
      price: '6999.00', originalPrice: '7999.00', image: 'sku1.jpg'
    },
    {
      id: 102, spuId: 1, name: '256GB 白色', spec: '{"color":"白色","storage":"256GB"}',
      price: '7999.00', originalPrice: null, image: ''
    },
  ]
}

export const MOCK_CATEGORIES_TREE = [
  {
    id: 1, name: '手机数码', parentId: 0, level: 1, sort: 1,
    children: [
      { id: 3, name: '智能手机', parentId: 1, level: 2, sort: 1, children: [] },
      { id: 4, name: '平板电脑', parentId: 1, level: 2, sort: 2, children: [] },
    ]
  },
  {
    id: 2, name: '服装鞋帽', parentId: 0, level: 1, sort: 2,
    children: [
      { id: 5, name: '男装', parentId: 2, level: 2, sort: 1, children: [] },
    ]
  },
]

export const MOCK_CATEGORIES_ALL = [
  { id: 1, name: '手机数码', parentId: 0, level: 1, sort: 1 },
  { id: 2, name: '服装鞋帽', parentId: 0, level: 1, sort: 2 },
  { id: 3, name: '智能手机', parentId: 1, level: 2, sort: 1 },
  { id: 4, name: '平板电脑', parentId: 1, level: 2, sort: 2 },
  { id: 5, name: '男装', parentId: 2, level: 2, sort: 1 },
]

export const MOCK_USERS = [
  { id: 10001, username: 'zhangsan', phone: '13800138000', avatar: null, createdAt: '2026-04-01T12:00:00' },
  { id: 10002, username: 'lisi', phone: null, avatar: null, createdAt: '2026-04-15T08:30:00' },
  { id: 10003, username: 'a_very_long_username_indeed', phone: '13900139000', avatar: null, createdAt: '2026-05-01T09:00:00' },
]

export const MOCK_STOCK = {
  id: 1, skuId: 101, totalStock: 500, lockedStock: 50, availableStock: 450
}
