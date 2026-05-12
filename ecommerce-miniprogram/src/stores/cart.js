import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { request } from '@/utils/api'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])

  const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  const totalAmount = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))
  const checkedItems = computed(() => items.value.filter(i => i.checked))
  const checkedAmount = computed(() => checkedItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

  async function fetchCart() {
    const res = await request({ url: '/api/v1/cart' })
    if (res.code === 200) {
      items.value = (res.data?.items || []).map(i => ({ ...i, checked: true }))
    }
  }

  async function addToCart(skuId, quantity = 1) {
    await request({ url: '/api/v1/cart', method: 'POST', data: { skuId, quantity } })
    await fetchCart()
  }

  async function updateQuantity(itemId, quantity) {
    if (quantity <= 0) { await removeItem(itemId); return }
    await request({ url: `/api/v1/cart/${itemId}`, method: 'PUT', data: { quantity } })
    await fetchCart()
  }

  async function removeItem(itemId) {
    await request({ url: `/api/v1/cart/${itemId}`, method: 'DELETE' })
    items.value = items.value.filter(i => i.id !== itemId)
  }

  function toggleCheck(itemId) {
    const item = items.value.find(i => i.id === itemId)
    if (item) item.checked = !item.checked
  }

  function toggleAll() {
    const allChecked = items.value.every(i => i.checked)
    items.value.forEach(i => i.checked = !allChecked)
  }

  return { items, totalCount, totalAmount, checkedItems, checkedAmount, fetchCart, addToCart, updateQuantity, removeItem, toggleCheck, toggleAll }
})
