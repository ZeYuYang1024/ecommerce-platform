<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">我的订单</h1>

    <div v-if="orders.length === 0 && !loading" class="mt-8 text-center py-16 text-gray-400">
      暂无订单
      <NuxtLink to="/" class="text-amber-600">去逛逛 →</NuxtLink>
    </div>

    <div v-else class="mt-8 space-y-4">
      <NuxtLink v-for="order in orders" :key="order.id" :to="`/user/orders/${order.orderNo}`" class="block bg-white rounded-2xl border border-gray-100 p-6 hover:shadow-md transition-shadow">
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
            <span v-if="order.status === 0" @click.stop="navigateTo(`/payment/${order.orderNo}`)" class="ml-4 text-sm px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white font-medium rounded-lg transition-colors cursor-pointer">去支付</span>
            <button v-if="order.status === 0" @click.stop="cancelOrder(order.id)" class="ml-2 text-sm text-red-400 hover:text-red-500">取消订单</button>
          </div>
        </div>
      </NuxtLink>

      <div v-if="total > size" class="flex justify-center items-center gap-2 pt-4">
        <button :disabled="page <= 1" @click="goPage(page - 1)" class="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-30 hover:bg-gray-50">上一页</button>
        <button v-for="p in pages" :key="p" @click="goPage(p)" :class="p === page ? 'bg-amber-500 text-white' : 'hover:bg-gray-50'" class="px-3 py-1.5 text-sm border rounded-lg">
          {{ p }}
        </button>
        <button :disabled="page * size >= total" @click="goPage(page + 1)" class="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-30 hover:bg-gray-50">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const orders = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const size = 10
const total = ref(0)

const pages = computed(() => {
  const count = Math.ceil(total.value / size)
  return Array.from({ length: count }, (_, i) => i + 1)
})

function statusClass(status: number) {
  const map: Record<number, string> = { 0: 'text-amber-600', 1: 'text-green-600', 2: 'text-blue-600', 3: 'text-gray-400', 4: 'text-red-400' }
  return map[status] || 'text-gray-400'
}

async function fetchOrders() {
  loading.value = true
  try {
    const api = useApi()
    const res: any = await api.get(`/orders?page=${page.value}&size=${size}`)
    if (res.code === 200) {
      orders.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally { loading.value = false }
}

function goPage(p: number) {
  page.value = p
  fetchOrders()
}

async function cancelOrder(id: number) {
  const api = useApi()
  await api.put(`/orders/${id}/cancel`)
  fetchOrders()
}

onMounted(() => fetchOrders())
</script>
