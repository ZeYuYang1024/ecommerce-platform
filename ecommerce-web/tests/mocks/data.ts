export const MOCK_PRODUCTS = {
  records: [
    { id: 1, name: 'iPhone 15 Pro', description: 'Apple 最新旗舰', mainImage: 'iphone.jpg', status: 1, reviewCount: 256, categoryId: 1 },
    { id: 2, name: 'MacBook Air M4', description: '轻薄本', mainImage: 'macbook.jpg', status: 1, reviewCount: 89, categoryId: 3 },
    { id: 3, name: 'AirPods Pro 2', description: '降噪耳机', mainImage: '', status: 1, reviewCount: 412, categoryId: 1 },
    { id: 4, name: 'iPad Pro', description: 'M4 芯片平板', mainImage: 'ipad.jpg', status: 1, reviewCount: 156, categoryId: 1 },
  ],
  total: 4
}

export const MOCK_PRODUCT_DETAIL = {
  spu: { id: 1, name: 'iPhone 15 Pro', description: 'A17 Pro 芯片', mainImage: 'iphone.jpg', status: 1, reviewCount: 256, avgRating: 4.8 },
  skus: [
    { id: 101, name: '128GB 黑色', spec: '{"color":"黑色","storage":"128GB"}', price: '6999.00', originalPrice: '7999.00', image: '' },
    { id: 102, name: '256GB 白色', spec: '{"color":"白色","storage":"256GB"}', price: '7999.00', originalPrice: null, image: '' },
  ]
}

export const MOCK_CART_ITEMS = [
  { skuId: 101, spuId: 1, name: 'iPhone 15 Pro - 128GB 黑色', image: '', price: '6999.00', quantity: 1, checked: true },
  { skuId: 201, spuId: 2, name: 'MacBook Air M4', image: 'macbook.jpg', price: '8999.00', quantity: 1, checked: true },
]

export const MOCK_ORDERS = [
  {
    id: 1, orderNo: '202605101200000001', totalAmount: 6999.00, status: 0, statusText: '待支付',
    receiverName: '收货人', receiverPhone: '13800138000', receiverAddress: '北京市朝阳区',
    items: [{ id: 1, name: 'iPhone 15 Pro', price: 6999.00, quantity: 1, totalPrice: 6999.00, image: '' }]
  },
  {
    id: 2, orderNo: '202605101200000002', totalAmount: 8999.00, status: 1, statusText: '已支付',
    receiverName: '收货人', receiverPhone: '13800138000', receiverAddress: '北京市朝阳区',
    items: [{ id: 2, name: 'MacBook Air M4', price: 8999.00, quantity: 1, totalPrice: 8999.00, image: 'macbook.jpg' }]
  }
]
