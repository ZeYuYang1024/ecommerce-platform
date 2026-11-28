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
                <span v-if="selectedAddr.isDefault" class="ml-2 text-xs bg-amber-200 text-amber-800 px-2 py-0.5 rounded-full font-medium">默认</span>
              </div>
              <div class="text-sm text-gray-400 mt-1">{{ selectedAddr.province }}{{ selectedAddr.city }}{{ selectedAddr.district }} {{ selectedAddr.detail }}</div>
            </div>
            <button @click="showAddrList = true" class="text-sm text-amber-600 hover:text-amber-700 font-medium flex-shrink-0 ml-4">管理地址</button>
          </div>

          <!-- First-time: no address -->
          <div v-else-if="!showAddrList && addresses.length === 0" class="text-center py-6 text-gray-400 text-sm">
            暂无地址，请添加收货地址
            <button @click="showAddrList = true; openAddressForm()" class="ml-2 text-amber-600 font-medium">+ 新建</button>
          </div>

          <!-- Expanded: address list + new form -->
          <div v-if="showAddrList">
            <div v-if="addresses.length > 0" class="space-y-2 mb-4">
              <div v-for="addr in addresses" :key="addr.id"
                :class="['block p-3 rounded-xl border-2 transition-all',
                  selectedAddr?.id === addr.id ? 'border-amber-400 bg-amber-50' : 'border-gray-100 hover:border-gray-200']">
                <label class="flex gap-2 cursor-pointer">
                  <input type="radio" :value="addr.id" v-model="selectedAddrId" @change="showAddrList = false" />
                <div class="flex justify-between">
                  <div>
                    <span class="font-medium text-sm text-gray-900">{{ addr.receiverName }}</span>
                    <span class="text-gray-400 ml-2 text-sm">{{ addr.receiverPhone }}</span>
                    <span v-if="addr.isDefault" class="ml-2 text-xs bg-amber-200 text-amber-800 px-1.5 py-0.5 rounded-full">默认</span>
                  </div>
                </div>
                </label>
                <div class="text-xs text-gray-600 mt-1">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</div>
                <div class="flex gap-4 mt-2 text-sm text-amber-700">
                  <button @click="openAddressForm(addr)" :disabled="savingAddr">编辑</button>
                  <button @click="deleteAddress(addr)" :disabled="savingAddr">删除</button>
                  <button v-if="!addr.isDefault" @click="setDefaultAddress(addr)" :disabled="savingAddr">设为默认</button>
                </div>
              </div>
            </div>

            <button @click="showAddressForm ? (showAddressForm = false) : openAddressForm()" class="text-sm text-amber-600 hover:text-amber-700 font-medium">
              {{ showAddressForm ? '取消' : '+ 新建地址' }}
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
          <p v-if="addressError" role="alert" class="mt-3 text-sm text-red-600">{{ addressError }}</p>
        </div> <!-- end address section -->

        <!-- Order Items -->
        <div class="bg-white rounded-2xl border border-gray-100 p-6">
          <h3 class="font-medium text-gray-900 mb-4">商品明细</h3>
          <p v-if="!cart.ready" class="text-sm text-gray-600" role="status">
            {{ cart.loadError || '正在加载购物车…' }}
            <button v-if="cart.loadError" @click="cart.fetchCart" class="text-amber-700">重试</button>
          </p>
          <p v-else-if="!checkedItems.length" class="text-sm text-gray-600">没有可结算的商品 <NuxtLink to="/cart" class="text-amber-700">返回购物车</NuxtLink></p>
          <label v-if="unavailableItems.length && checkedItems.length" class="flex gap-2 mb-3 text-sm text-amber-800">
            <input type="checkbox" v-model="acceptAvailableItems" />
            {{ unavailableItems.length }} 件已选商品无法购买，我要继续结算其余商品
          </label>
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
        <button @click="submitOrder" :disabled="!canSubmit" class="mt-6 w-full h-12 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 disabled:text-gray-400 text-white font-medium rounded-xl transition-colors">
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
const addresses = ref<Address[]>([])
type Address = {
  id: string | number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
}
const selectedAddrId = ref<Address['id'] | null>(null)
const editingAddress = ref<Address | null>(null)
const addressError = ref('')
const emptyAddress = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: '' }
const newAddr = reactive({ ...emptyAddress })
const acceptAvailableItems = ref(false)

const checkedItems = computed(() => cart.items.filter(item => item.checked && item.available !== false))
const unavailableItems = computed(() => cart.items.filter(item => item.checked && item.available === false))
watch(() => unavailableItems.value.map(item => item.skuId).sort().join(','), () => {
  acceptAvailableItems.value = false
})
const totalPrice = computed(() => checkedItems.value.reduce((s: number, i: any) => s + i.price * i.quantity, 0).toFixed(2))
const selectedAddr = computed(() => addresses.value.find(a => a.id === selectedAddrId.value))
const hasAddress = computed(() => !!selectedAddrId.value)

const canSubmit = computed(() => !submitting.value && cart.ready && hasAddress.value
  && checkedItems.value.length > 0 && (!unavailableItems.value.length || acceptAvailableItems.value))

onMounted(() => {
  if (auth.isLoggedIn) void cart.fetchCart()
})
watch(() => auth.token, () => {
  addresses.value = []
  selectedAddrId.value = null
  showAddrList.value = false
  showAddressForm.value = false
  addressError.value = ''
  if (auth.isLoggedIn && import.meta.client) void loadAddresses()
}, { immediate: true })
watch(() => checkedItems.value, async items => {
  await primeImageUrls(items.map(item => item.image))
  await Promise.all(items.map(async item => {
    item.resolvedImage = await resolveImageUrl(item.image)
  }))
}, { immediate: true })

async function loadAddresses(preferredId = selectedAddrId.value) {
  const token = auth.token
  try {
    const res: any = await api.get('/users/addresses', { t: Date.now() })
    if (token !== auth.token) return
    if (res.code !== 200) throw new Error(res.message || '收货地址加载失败')
    addresses.value = res.data || []
    const preferred = addresses.value.find(address => address.id === preferredId)
    const fallback = addresses.value.find(address => address.isDefault) || addresses.value[0]
    selectedAddrId.value = (preferred || fallback)?.id ?? null
  } catch (error) {
    if (token === auth.token) addressError.value = error instanceof Error ? error.message : '收货地址加载失败'
  }
}

function openAddressForm(address: Address | null = null) {
  editingAddress.value = address
  addressError.value = ''
  for (const key of Object.keys(emptyAddress) as (keyof typeof emptyAddress)[]) {
    newAddr[key] = address?.[key] || ''
  }
  showAddressForm.value = true
}

async function changeAddress(change: () => Promise<any>, onSuccess: (data: any) => Promise<void>) {
  if (savingAddr.value) return
  const token = auth.token
  savingAddr.value = true
  addressError.value = ''
  try {
    const response = await change()
    if (token !== auth.token) return
    if (response.code !== 200) throw new Error(response.message || '地址操作失败')
    await onSuccess(response.data)
  } catch (error) {
    if (token === auth.token) addressError.value = error instanceof Error ? error.message : '地址操作失败'
  } finally {
    savingAddr.value = false
  }
}

async function saveAddress() {
  if (!newAddr.receiverName.trim() || !newAddr.receiverPhone.trim() || !newAddr.detail.trim()) {
    addressError.value = '请填写收货人、手机号和详细地址'
    return
  }
  const editing = editingAddress.value
  await changeAddress(
    () => editing
      ? api.put(`/users/addresses/${editing.id}`, { ...newAddr, isDefault: editing.isDefault })
      : api.post('/users/addresses', { ...newAddr }),
    async saved => {
      showAddressForm.value = false
      await loadAddresses(saved?.id ?? selectedAddrId.value)
    }
  )
}

async function deleteAddress(address: Address) {
  if (!window.confirm('删除此收货地址？')) return
  await changeAddress(() => api.delete(`/users/addresses/${address.id}`), async () => {
    if (editingAddress.value?.id === address.id) showAddressForm.value = false
    await loadAddresses()
  })
}

async function setDefaultAddress(address: Address) {
  await changeAddress(() => api.put(`/users/addresses/${address.id}/default`), () => loadAddresses())
}

const checkoutStorageKey = computed(() => `pending-checkout:${auth.username || ''}`)

function checkoutRequestId(address: any, items: { skuId: string | number; quantity: number }[]) {
  const signature = JSON.stringify([address.id, address.receiverName, address.receiverPhone, address.province, address.city, address.district, address.detail, ...items.map(item => [String(item.skuId), item.quantity]).sort()])
  const saved = sessionStorage.getItem(checkoutStorageKey.value)
  if (saved) {
    const previous = JSON.parse(saved)
    if (previous.signature === signature) return previous.id
  }
  const id = Array.from(crypto.getRandomValues(new Uint8Array(16)), value => value.toString(16).padStart(2, '0')).join('')
  sessionStorage.setItem(checkoutStorageKey.value, JSON.stringify({ signature, id }))
  return id
}

async function submitOrder() {
  const addr = selectedAddr.value
  if (!addr || !canSubmit.value) return
  const token = auth.token
  submitting.value = true
  orderError.value = ''
  try {
    const items = checkedItems.value.map((i: any) => ({
      skuId: i.skuId, spuId: i.spuId, name: i.name, image: i.image, price: i.price, quantity: i.quantity
    }))
    const res: any = await api.post('/orders', {
      receiverName: addr.receiverName,
      receiverPhone: addr.receiverPhone,
      receiverAddress: `${addr.province}${addr.city}${addr.district} ${addr.detail}`,
      items,
      clientRequestId: checkoutRequestId(addr, items)
    })
    if (token !== auth.token) return
    if (res.code === 200) {
      if ([4, 5].includes(res.data.status)) {
        sessionStorage.removeItem(checkoutStorageKey.value)
        orderError.value = '上次结算的订单已关闭，请重新提交'
        return
      }
      void cart.removePurchased(items).catch(() => {})
      const orderNo = encodeURIComponent(res.data.orderNo)
      await router.push(res.data.status === 0 ? `/payment/${orderNo}` : `/user/orders/${orderNo}`)
      sessionStorage.removeItem(checkoutStorageKey.value)
    } else {
      if (res.code === 40010020) sessionStorage.removeItem(checkoutStorageKey.value)
      orderError.value = res.message || '下单失败，请重试'
    }
  } catch {
    orderError.value = '网络错误，请稍后重试'
  } finally { submitting.value = false }
}
</script>
