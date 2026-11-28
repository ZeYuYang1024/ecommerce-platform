import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', () => {
  const items = ref<any[]>([])
  const count = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const ready = ref(false)
  const loadError = ref('')
  const auth = useAuthStore()
  const api = useApi()
  let fetchSequence = 0

  watch(() => auth.token, token => {
    fetchSequence++
    items.value = []
    ready.value = false
    loadError.value = ''
    if (token && import.meta.client) void fetchCart()
  }, { flush: 'sync' })

  async function fetchCart() {
    if (!auth.isLoggedIn) return false
    const sequence = ++fetchSequence
    try {
      const res: any = await api.get('/cart')
      if (sequence !== fetchSequence) return false
      if (res.code !== 200) throw new Error(res.message || '购物车加载失败')
      items.value = Array.isArray(res.data) ? res.data : []
      ready.value = true
      loadError.value = ''
      return true
    } catch {
      if (sequence === fetchSequence) {
        ready.value = false
        loadError.value = '购物车加载失败，请重试'
      }
      return false
    }
  }

  async function addItem(item: any) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      navigateTo('/login')
      return
    }
    const api = useApi()
    const result: any = await api.post('/cart/items', item)
    if (result.code !== 200) throw new Error(result.message || '加入购物车失败')
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

  async function removePurchased(purchased: { skuId: string | number; quantity: number }[]) {
    const api = useApi()
    const token = auth.token
    const response: any = await api.get('/cart')
    if (response.code !== 200 || token !== auth.token) return
    const current = new Map<string, number>(
      (response.data || []).map((item: any) => [String(item.skuId), item.quantity])
    )
    try {
      await Promise.allSettled(purchased.map(item => {
        const skuId = encodeURIComponent(String(item.skuId))
        const quantity = current.get(String(item.skuId))
        if (quantity === undefined) return
        const remaining = quantity - item.quantity
        return remaining > 0
          ? api.put(`/cart/items/${skuId}`, { quantity: remaining })
          : api.delete(`/cart/items/${skuId}`)
      }))
    } finally {
      await fetchCart()
    }
  }

  async function clearCart() {
    const api = useApi()
    const token = auth.token
    await api.delete('/cart')
    if (token === auth.token) await fetchCart()
  }

  return { items, count, ready, loadError, fetchCart, addItem, updateQuantity, removeItem, toggleCheck, clearCart, removePurchased }
})
