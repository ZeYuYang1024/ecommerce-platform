import { ref, computed } from 'vue'
import { request } from '@/utils/api'

const items = ref([])
const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
const totalAmount = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))
const checkedItems = computed(() => items.value.filter(i => i.checked))
const checkedAmount = computed(() => checkedItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

async function fetchCart() {
  const res = await request({ url: '/api/v1/cart' })
  if (res.code === 200) {
    const list = Array.isArray(res.data) ? res.data : (res.data?.items || [])
    items.value = list.map(i => ({ ...i, checked: i.checked !== false }))
  }
}

async function addToCart(skuId, quantity = 1) {
  await request({ url: '/api/v1/cart/items', method: 'POST', data: { skuId, quantity } })
  await fetchCart()
}

async function updateQuantity(skuId, quantity) {
  if (quantity <= 0) { await removeItem(skuId); return }
  await request({ url: `/api/v1/cart/items/${skuId}`, method: 'PUT', data: { quantity } })
  await fetchCart()
}

async function removeItem(skuId) {
  await request({ url: `/api/v1/cart/items/${skuId}`, method: 'DELETE' })
  items.value = items.value.filter(i => i.skuId !== skuId)
}

function toggleCheck(skuId) {
  const item = items.value.find(i => i.skuId === skuId)
  if (item) item.checked = !item.checked
}

function toggleAll() {
  const allChecked = items.value.every(i => i.checked)
  items.value.forEach(i => { i.checked = !allChecked })
}

function removeCheckedItems() {
  items.value = items.value.filter(i => !i.checked)
}

export function useCartStore() {
  return {
    get items() { return items.value },
    set items(value) { items.value = value },
    get totalCount() { return totalCount.value },
    get totalAmount() { return totalAmount.value },
    get checkedItems() { return checkedItems.value },
    get checkedAmount() { return checkedAmount.value },
    fetchCart,
    addToCart,
    updateQuantity,
    removeItem,
    toggleCheck,
    toggleAll,
    removeCheckedItems
  }
}
