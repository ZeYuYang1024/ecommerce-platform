<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <NuxtLink to="/user/orders" class="text-sm text-gray-400 hover:text-amber-600 mb-6 inline-block">← 返回订单列表</NuxtLink>
    <h1 class="text-2xl font-bold text-gray-900">订单详情</h1>
    <div v-if="loading" class="mt-8 text-center py-16 text-gray-400">加载中...</div>
    <div v-else-if="errorMsg" class="mt-8 text-center py-16 text-red-400">{{ errorMsg }}</div>
    <div v-else-if="order" class="mt-8 space-y-6">
      <div class="bg-white rounded-2xl border border-gray-100 p-6">
        <div class="flex justify-between items-center mb-4">
          <span class="text-sm text-gray-400 font-mono">订单号: {{ order.orderNo }}</span>
          <span :class="statusClass(order.status)" class="text-sm font-medium">{{ order.statusText }}</span>
        </div>
        <div class="text-sm text-gray-500 space-y-1">
          <div>收货人: {{ order.receiverName }} {{ order.receiverPhone }}</div>
          <div>收货地址: {{ order.receiverAddress }}</div>
          <div>下单时间: {{ order.createdAt }}</div>
        </div>
      </div>

      <div class="bg-white rounded-2xl border border-gray-100 p-6">
        <h3 class="font-medium text-gray-900 mb-4">商品明细</h3>
        <div v-for="item in order.items" :key="item.id" class="flex items-center gap-4 py-3 border-b border-gray-50 last:border-0">
          <div class="w-14 h-14 bg-gray-50 rounded-lg overflow-hidden">
            <img :src="item.resolvedImage || '/placeholder.svg'" class="w-full h-full object-cover" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium truncate">{{ item.name }}</div>
            <div class="text-xs text-gray-400">¥{{ item.price }} × {{ item.quantity }}</div>
          </div>
          <div class="text-sm font-medium">¥{{ item.totalPrice }}</div>
        </div>
        <div class="mt-4 pt-4 border-t border-gray-100 flex justify-between">
          <span class="font-medium">合计</span>
          <span class="text-xl font-bold text-amber-600">¥{{ order.totalAmount }}</span>
        </div>
        <div v-if="order.status === 0" class="mt-4 flex gap-3">
          <NuxtLink :to="`/payment/${order.orderNo}`" class="flex-1 text-center py-3 bg-amber-500 text-white font-medium rounded-xl hover:bg-amber-600 transition-colors">去支付</NuxtLink>
          <button @click="cancel" class="px-6 py-3 text-red-400 hover:text-red-500 font-medium">取消订单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const api = useApi()
const order = ref<any>(null)
const loading = ref(true)
const errorMsg = ref('')
const { primeImageUrls, resolveImageUrl } = useImageUrl()

onMounted(async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const res: any = await api.get(`/orders/no/${route.params.id}`)
    if (res.code === 200) {
      order.value = res.data
      const items = order.value?.items || []
      await primeImageUrls(items.map((item: any) => item.image))
      await Promise.all(items.map(async (item: any) => {
        item.resolvedImage = await resolveImageUrl(item.image)
      }))
    } else {
      errorMsg.value = res.message || '订单不存在'
    }
  } catch (e: any) {
    errorMsg.value = e.message || '网络错误'
  } finally {
    loading.value = false
  }
})

function statusClass(status: number) {
  const map: Record<number, string> = { 0: 'text-amber-600', 1: 'text-green-600', 2: 'text-blue-600', 3: 'text-gray-400', 4: 'text-red-400' }
  return map[status] || 'text-gray-400'
}

async function cancel() {
  await api.put(`/orders/${order.value.id}/cancel`)
  order.value.status = 4
  order.value.statusText = '已取消'
}
</script>
