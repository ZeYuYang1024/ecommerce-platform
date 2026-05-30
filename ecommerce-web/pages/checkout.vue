<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">确认订单</h1>

    <div v-if="!auth.isLoggedIn" class="mt-8 text-center py-16 text-gray-400">
      请先登录 <NuxtLink to="/login" class="text-amber-600">去登录</NuxtLink>
    </div>

    <div v-else class="mt-8 grid md:grid-cols-3 gap-8">
      <div class="md:col-span-2 space-y-6">
        <!-- Address: default compact, click to expand -->
        <div class="bg-white rounded-2xl border border-gray-100 p-6">
          <h3 class="font-medium text-gray-900 mb-3">收货地址</h3>

          <!-- Default address: compact view -->
          <div v-if="!showAddrList && selectedAddr" class="flex items-center justify-between p-3 bg-amber-50 rounded-xl border border-amber-200">
            <div>
              <div>
                <span class="font-medium text-gray-900">{{ selectedAddr.receiverName }}</span>
                <span class="text-gray-400 ml-3">{{ selectedAddr.receiverPhone }}</span>
                <span class="ml-2 text-xs bg-amber-200 text-amber-800 px-2 py-0.5 rounded-full font-medium">默认</span>
              </div>
              <div class="text-sm text-gray-400 mt-1">{{ selectedAddr.province }}{{ selectedAddr.city }}{{ selectedAddr.district }} {{ selectedAddr.detail }}</div>
            </div>
            <button @click="showAddrList = true" class="text-sm text-amber-600 hover:text-amber-700 font-medium flex-shrink-0 ml-4">切换地址</button>
          </div>

          <!-- First-time: no address -->
          <div v-else-if="!showAddrList && addresses.length === 0" class="text-center py-6 text-gray-400 text-sm">
            暂无地址，请添加收货地址
            <button @click="showAddrList = true; showAddressForm = true" class="ml-2 text-amber-600 font-medium">+ 新建</button>
          </div>

          <!-- Expanded: address list + new form -->
          <div v-if="showAddrList">
            <div v-if="addresses.length > 0" class="space-y-2 mb-4">
              <label v-for="addr in addresses" :key="addr.id"
                :class="['block p-3 rounded-xl border-2 cursor-pointer transition-all',
                  selectedAddr?.id === addr.id ? 'border-amber-400 bg-amber-50' : 'border-gray-100 hover:border-gray-200']">
                <input type="radio" :value="addr.id" v-model="selectedAddrId" @change="showAddrList = false" class="sr-only" />
                <div class="flex justify-between">
                  <div>
                    <span class="font-medium text-sm text-gray-900">{{ addr.receiverName }}</span>
                    <span class="text-gray-400 ml-2 text-sm">{{ addr.receiverPhone }}</span>
                    <span v-if="addr.isDefault" class="ml-2 text-xs bg-amber-200 text-amber-800 px-1.5 py-0.5 rounded-full">默认</span>
                  </div>
                </div>
                <div class="text-xs text-gray-400 mt-1">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</div>
              </label>
            </div>

            <button @click="showAddressForm = !showAddressForm" class="text-sm text-amber-600 hover:text-amber-700 font-medium">
              {{ showAddressForm ? '取消添加' : '+ 新建地址' }}
            </button>

          <!-- New Address Form -->
          <div v-if="showAddressForm" class="space-y-3">
            <div class="grid grid-cols-2 gap-3">
              <input v-model="newAddr.receiverName" placeholder="收货人" class="h-11 px-4 rounded-xl border border-gray-200 text-sm" />
              <input v-model="newAddr.receiverPhone" placeholder="手机号" class="h-11 px-4 rounded-xl border border-gray-200 text-sm" />
            </div>
            <div class="grid grid-cols-3 gap-3">
              <input v-model="newAddr.province" placeholder="省" class="h-11 px-4 rounded-xl border border-gray-200 text-sm" />
              <input v-model="newAddr.city" placeholder="市" class="h-11 px-4 rounded-xl border border-gray-200 text-sm" />
              <input v-model="newAddr.district" placeholder="区" class="h-11 px-4 rounded-xl border border-gray-200 text-sm" />
            </div>
            <input v-model="newAddr.detail" placeholder="详细地址（街道/门牌号）" class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm" />
            <button @click="saveAddress" :disabled="savingAddr" class="w-full h-11 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 text-white font-medium rounded-xl transition-colors text-sm">
              {{ savingAddr ? '保存中...' : '保存地址' }}
            </button>
          </div>
          </div> <!-- end showAddrList -->
        </div> <!-- end address section -->

        <!-- Order Items -->
        <div class="bg-white rounded-2xl border border-gray-100 p-6">
          <h3 class="font-medium text-gray-900 mb-4">商品明细</h3>
          <div v-for="item in checkedItems" :key="item.skuId" class="flex items-center gap-4 py-3 border-b border-gray-50 last:border-0">
            <div class="w-12 h-12 bg-gray-50 rounded-lg overflow-hidden flex-shrink-0">
              <img :src="item.resolvedImage || '/placeholder.svg'" class="w-full h-full object-cover" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium truncate">{{ item.name }}</div>
              <div class="text-xs text-gray-400">¥{{ item.price }} x {{ item.quantity }}</div>
            </div>
            <div class="text-sm font-medium">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          </div>
        </div>
      </div>

      <!-- Summary -->
      <div class="bg-white rounded-2xl border border-gray-100 p-6 h-fit sticky top-20">
        <div class="text-sm text-gray-500 flex justify-between py-2">
          <span>商品金额</span><span>¥{{ totalPrice }}</span>
        </div>
        <div class="border-t border-gray-100 mt-2 pt-4 flex justify-between">
          <span class="font-medium">应付金额</span>
          <span class="text-xl font-bold text-amber-600">¥{{ totalPrice }}</span>
        </div>
        <button @click="submitOrder" :disabled="submitting || !hasAddress" class="mt-6 w-full h-12 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 disabled:text-gray-400 text-white font-medium rounded-xl transition-colors">
          {{ submitting ? '提交中...' : !hasAddress ? '请选择收货地址' : '提交订单' }}
        </button>
        <p v-if="orderError" class="mt-2 text-sm text-red-500 text-center">{{ orderError }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()
const api = useApi()
const { primeImageUrls, resolveImageUrl } = useImageUrl()

const submitting = ref(false)
const orderError = ref('')
const savingAddr = ref(false)
const showAddrList = ref(false)
const showAddressForm = ref(false)
const addresses = ref<any[]>([])
const selectedAddrId = ref<number | null>(null)
const newAddr = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: '' })

const checkedItems = computed(() => cart.items.filter((i: any) => i.checked))
const totalPrice = computed(() => checkedItems.value.reduce((s: number, i: any) => s + i.price * i.quantity, 0).toFixed(2))
const selectedAddr = computed(() => addresses.value.find(a => a.id === selectedAddrId.value))
const hasAddress = computed(() => !!selectedAddrId.value)

onMounted(async () => {
  await primeImageUrls(cart.items.map((item: any) => item.image))
  await Promise.all(cart.items.map(async (item: any) => {
    item.resolvedImage = await resolveImageUrl(item.image)
  }))
  try {
    const res: any = await api.get('/users/addresses?t=' + Date.now())
    if (res.code === 200 && res.data) {
      addresses.value = res.data
      const def = res.data.find((a: any) => a.isDefault)
      if (def) selectedAddrId.value = def.id
      else if (res.data.length > 0) selectedAddrId.value = res.data[0].id
    }
  } catch { addresses.value = [] }
})

async function saveAddress() {
  if (!newAddr.receiverName || !newAddr.receiverPhone || !newAddr.detail) return
  savingAddr.value = true
  try {
    const res: any = await api.post('/users/addresses', { ...newAddr })
    if (res.code === 200) {
      addresses.value.unshift(res.data)
      selectedAddrId.value = res.data.id
      showAddressForm.value = false
      Object.assign(newAddr, { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: '' })
    }
  } finally { savingAddr.value = false }
}

async function submitOrder() {
  const addr = selectedAddr.value
  if (!addr) return
  submitting.value = true
  try {
    const items = checkedItems.value.map((i: any) => ({
      skuId: i.skuId, spuId: i.spuId, name: i.name, image: i.image, price: i.price, quantity: i.quantity
    }))
    const res: any = await api.post('/orders', {
      receiverName: addr.receiverName,
      receiverPhone: addr.receiverPhone,
      receiverAddress: `${addr.province}${addr.city}${addr.district} ${addr.detail}`,
      items
    })
    if (res.code === 200) {
      await cart.clearCart()
      router.push(`/payment/${res.data.orderNo}`)
    } else {
      orderError.value = res.message || '下单失败，请重试'
    }
  } catch {
    orderError.value = '网络错误，请稍后重试'
  } finally { submitting.value = false }
}
</script>
