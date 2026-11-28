import { ref, computed, watch } from 'vue'
import { request } from '@/utils/api'
import { token } from '@/utils/session'

const items = ref([])
const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
const checkedItems = computed(() => items.value.filter(item => item.checked && isPurchasable(item)))
const checkedAmount = computed(() => checkedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0))
let fetchSequence = 0

watch(token, () => {
  fetchSequence++
  items.value = []
}, { flush: 'sync' })

export function isPurchasable(item) {
  return item.available !== false && item.skuId != null && item.spuId != null && Boolean(item.name)
    && item.price != null && Number.isFinite(Number(item.price))
}

async function fetchCart() {
  const sequence = ++fetchSequence
  if (!token.value) return { code: 401, message: '请先登录' }
  const res = await request({ url: '/api/v1/cart' })
  if (res.code === 200 && sequence === fetchSequence) {
    const list = Array.isArray(res.data) ? res.data : (res.data?.items || [])
    items.value = list.map(item => ({ ...item, checked: item.checked !== false }))
  }
  return res
}

async function addToCart(item) {
  const res = await request({ url: '/api/v1/cart/items', method: 'POST', data: item })
  if (res.code === 200) await fetchCart()
  return res
}

async function updateQuantity(skuId, quantity) {
  if (quantity <= 0) return removeItem(skuId)
  const res = await request({ url: `/api/v1/cart/items/${skuId}`, method: 'PUT', data: { quantity } })
  if (res.code === 200) await fetchCart()
  return res
}

async function removeItem(skuId) {
  const res = await request({ url: `/api/v1/cart/items/${skuId}`, method: 'DELETE' })
  if (res.code === 200) items.value = items.value.filter(item => item.skuId !== skuId)
  return res
}

async function toggleCheck(skuId) {
  const res = await request({ url: `/api/v1/cart/items/${skuId}/check`, method: 'PUT' })
  if (res.code === 200) await fetchCart()
  return res
}

async function toggleAll() {
  const candidates = items.value.filter(isPurchasable)
  const nextChecked = !candidates.every(item => item.checked)
  const updates = await Promise.all(candidates.filter(item => item.checked !== nextChecked).map(item =>
    request({ url: `/api/v1/cart/items/${item.skuId}/check`, method: 'PUT' })
  ))
  await fetchCart()
  return updates.find(res => res.code !== 200) || { code: 200 }
}

async function removePurchased(purchased) {
  const res = await fetchCart()
  if (res.code !== 200) return false
  const current = new Map(items.value.map(item => [item.skuId, item]))
  const changes = await Promise.all(purchased.map(item => {
    const match = current.get(item.skuId)
    if (!match) return { code: 200 }
    const remaining = match.quantity - item.quantity
    return request({
      url: `/api/v1/cart/items/${item.skuId}`,
      method: remaining > 0 ? 'PUT' : 'DELETE',
      data: remaining > 0 ? { quantity: remaining } : undefined
    })
  }))
  await fetchCart()
  return changes.every(item => item.code === 200)
}

export function useCartStore() {
  return {
    get items() { return items.value },
    get totalCount() { return totalCount.value },
    get checkedItems() { return checkedItems.value },
    get checkedAmount() { return checkedAmount.value },
    fetchCart,
    addToCart,
    updateQuantity,
    removeItem,
    toggleCheck,
    toggleAll,
    removePurchased
  }
}
