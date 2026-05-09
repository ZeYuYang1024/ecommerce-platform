<template>
  <div class="max-w-sm mx-auto px-4 py-20">
    <h1 class="text-2xl font-bold text-center text-gray-900">注册</h1>
    <div class="mt-8 space-y-4">
      <input v-model="form.username" placeholder="用户名（3-32位）" class="w-full h-12 px-4 rounded-xl border border-gray-200 text-sm" />
      <input v-model="form.password" type="password" placeholder="密码（6-32位）" class="w-full h-12 px-4 rounded-xl border border-gray-200 text-sm" />
      <button @click="doRegister" :disabled="loading" class="w-full h-12 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 text-white font-medium rounded-xl transition-colors">
        {{ loading ? '注册中...' : '注 册' }}
      </button>
      <p class="text-sm text-gray-400 text-center">
        已有账号？<NuxtLink to="/login" class="text-amber-600">去登录</NuxtLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function doRegister() {
  loading.value = true
  try {
    const ok = await auth.register(form)
    if (ok) router.push('/')
  } finally { loading.value = false }
}
</script>
