import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', () => {
  const items = ref<any[]>([])
  const count = ref(0)

  async function fetchCart() {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) return
    const api = useApi()
    try {
      const res: any = await api.get('/cart')
      if (res.code === 200) {
        items.value = res.data || []
        count.value = items.value.reduce((s: number, i: any) => s + i.quantity, 0)
      }
    } catch {}
  }

  async function addItem(item: any) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      navigateTo('/login')
      return
    }
    const api = useApi()
    await api.post('/cart/items', item)
    await fetchCart()
  }

  async function updateQuantity(skuId: number, quantity: number) {
    const api = useApi()
    await api.put(`/cart/items/${skuId}`, { quantity })
    await fetchCart()
  }

  async function removeItem(skuId: number) {
    const api = useApi()
    await api.delete(`/cart/items/${skuId}`)
    await fetchCart()
  }

  async function toggleCheck(skuId: number) {
    const api = useApi()
    await api.put(`/cart/items/${skuId}/check`)
    await fetchCart()
  }

  async function clearCart() {
    const api = useApi()
    await api.delete('/cart')
    items.value = []
    count.value = 0
  }

  return { items, count, fetchCart, addItem, updateQuantity, removeItem, toggleCheck, clearCart }
})
