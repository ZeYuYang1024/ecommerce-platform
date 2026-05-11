<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">我的订单</h1>

    <div v-if="orders.length === 0 && !loading" class="mt-8 text-center py-16 text-gray-400">
      暂无订单
      <NuxtLink to="/" class="text-amber-600">去逛逛 →</NuxtLink>
    </div>

    <div v-else class="mt-8 space-y-4">
      <div v-for="order in orders" :key="order.id" class="bg-white rounded-2xl border border-gray-100 p-6 cursor-pointer hover:shadow-md transition-shadow" @click="navigateTo(`/user/orders/${order.orderNo}`)">
        <div class="flex justify-between items-center mb-4">
          <span class="text-sm text-gray-400 font-mono">订单号: {{ order.orderNo }}</span>
          <span :class="statusClass(order.status)" class="text-sm font-medium">{{ order.statusText }}</span>
        </div>
        <div v-for="item in order.items" :key="item.id" class="flex items-center gap-4 py-2 border-b border-gray-50 last:border-0">
          <div class="w-14 h-14 bg-gray-50 rounded-lg overflow-hidden">
            <img :src="item.image || '/placeholder.svg'" class="w-full h-full object-cover" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium truncate">{{ item.name }}</div>
            <div class="text-xs text-gray-400">¥{{ item.price }} × {{ item.quantity }}</div>
          </div>
          <div class="text-sm font-medium">¥{{ item.totalPrice }}</div>
        </div>
        <div class="mt-4 flex justify-between items-center">
          <span class="text-sm text-gray-500">共 {{ order.items?.length || 0 }} 件</span>
          <div>
            <span class="text-lg font-bold text-amber-600">¥{{ order.totalAmount }}</span>
            <NuxtLink v-if="order.status === 0" :to="`/payment/${order.orderNo}`" @click.stop class="ml-4 text-sm px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white font-medium rounded-lg transition-colors">去支付</NuxtLink>
            <button v-if="order.status === 0" @click.stop="cancelOrder(order.id)" class="ml-2 text-sm text-red-400 hover:text-red-500">取消订单</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const orders = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const api = useApi()
    const res: any = await api.get('/orders')
    if (res.code === 200) orders.value = res.data?.records || []
  } finally { loading.value = false }
})

function statusClass(status: number) {
  const map: Record<number, string> = { 0: 'text-amber-600', 1: 'text-green-600', 2: 'text-blue-600', 3: 'text-gray-400', 4: 'text-red-400' }
  return map[status] || 'text-gray-400'
}

async function cancelOrder(id: number) {
  const api = useApi()
  await api.put(`/orders/${id}/cancel`)
  const res: any = await api.get('/orders')
  if (res.code === 200) orders.value = res.data?.records || []
}
</script>
