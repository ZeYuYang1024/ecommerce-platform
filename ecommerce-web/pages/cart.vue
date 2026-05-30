<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">购物车</h1>

    <div v-if="!auth.isLoggedIn" class="mt-8 text-center py-16">
      <p class="text-gray-400 text-lg">请先登录后查看购物车</p>
      <NuxtLink to="/login" class="mt-4 inline-block px-8 py-3 bg-amber-500 text-white font-medium rounded-xl hover:bg-amber-600 transition-colors">去登录</NuxtLink>
    </div>

    <div v-else-if="cart.items.length === 0" class="mt-8 text-center py-16">
      <p class="text-gray-400 text-lg">购物车是空的</p>
      <NuxtLink to="/" class="mt-4 inline-block text-amber-600 hover:text-amber-700 font-medium">去逛逛 →</NuxtLink>
    </div>

    <div v-else class="mt-8 space-y-4">
      <div v-for="item in cart.items" :key="item.skuId" class="bg-white rounded-2xl border border-gray-100 p-4 flex items-center gap-4">
        <input type="checkbox" :checked="item.checked" @change="cart.toggleCheck(item.skuId)" class="w-5 h-5 accent-amber-500" />
        <div class="w-20 h-20 bg-gray-50 rounded-xl overflow-hidden flex-shrink-0">
          <img :src="item.resolvedImage || '/placeholder.svg'" class="w-full h-full object-cover" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="font-medium text-gray-900 truncate">{{ item.name }}</div>
          <div class="text-sm text-gray-400 mt-1">¥{{ item.price }}</div>
        </div>
        <div class="flex items-center gap-2">
          <button @click="decrease(item)" class="w-8 h-8 rounded-lg border border-gray-200 hover:bg-gray-50">−</button>
          <span class="w-10 text-center font-medium">{{ item.quantity }}</span>
          <button @click="increase(item)" class="w-8 h-8 rounded-lg border border-gray-200 hover:bg-gray-50">+</button>
        </div>
        <div class="text-lg font-bold text-amber-600 w-24 text-right">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
        <button @click="cart.removeItem(item.skuId)" class="text-gray-300 hover:text-red-400 transition-colors">
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
        </button>
      </div>

      <div class="bg-white rounded-2xl border border-gray-100 p-6 flex justify-between items-center">
        <div>
          <span class="text-gray-500 text-sm">已选商品合计</span>
          <div class="text-2xl font-bold text-amber-600">¥{{ totalPrice }}</div>
        </div>
        <button @click="checkout" class="px-10 py-3 bg-amber-500 hover:bg-amber-600 text-white font-medium rounded-xl transition-colors">去结算</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const cart = useCartStore()
const { primeImageUrls, resolveImageUrl } = useImageUrl()

const totalPrice = computed(() => {
  return cart.items
    .filter((i: any) => i.checked)
    .reduce((s: number, i: any) => s + i.price * i.quantity, 0)
    .toFixed(2)
})

watch(() => cart.items.map((item: any) => item.image), async () => {
  await primeImageUrls(cart.items.map((item: any) => item.image))
  await Promise.all(cart.items.map(async (item: any) => {
    item.resolvedImage = await resolveImageUrl(item.image)
  }))
}, { immediate: true })

function increase(item: any) { cart.updateQuantity(item.skuId, item.quantity + 1) }
function decrease(item: any) {
  if (item.quantity > 1) cart.updateQuantity(item.skuId, item.quantity - 1)
}

function checkout() {
  const checked = cart.items.filter((i: any) => i.checked)
  if (checked.length === 0) return
  navigateTo('/checkout')
}
</script>
