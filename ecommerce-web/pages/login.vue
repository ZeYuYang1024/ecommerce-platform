<template>
  <div class="max-w-sm mx-auto px-4 py-20">
    <h1 class="text-2xl font-bold text-center text-gray-900">登录</h1>
    <div class="mt-8 space-y-4">
      <input v-model="form.username" placeholder="用户名" class="w-full h-12 px-4 rounded-xl border border-gray-200 text-sm" />
      <input v-model="form.password" type="password" placeholder="密码" class="w-full h-12 px-4 rounded-xl border border-gray-200 text-sm" />
      <button @click="doLogin" :disabled="loading" class="w-full h-12 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 text-white font-medium rounded-xl transition-colors">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <p v-if="error" class="text-sm text-red-500 text-center">{{ error }}</p>
      <p class="text-sm text-gray-400 text-center">
        还没有账号？<NuxtLink to="/register" class="text-amber-600">立即注册</NuxtLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({ username: '', password: '' })

async function doLogin() {
  error.value = ''
  loading.value = true
  try {
    const ok = await auth.login(form)
    if (ok) router.push('/')
    else error.value = '登录失败，请检查用户名和密码'
  } catch { error.value = '网络错误' }
  finally { loading.value = false }
}
</script>
