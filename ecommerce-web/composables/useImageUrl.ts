import { toValue, type MaybeRefOrGetter } from 'vue'

export const useImageUrl = () => {
  const api = useApi()
  const resolvedMap = useState<Record<string, string>>('image-url-map', () => ({}))
  const pendingMap = useState<Record<string, Promise<string>>>('image-url-pending', () => ({}))

  async function resolveImageUrl(src?: string | null): Promise<string> {
    if (!src) return '/placeholder.svg'
    if (src.startsWith('http') || src.startsWith('/')) return src

    if (resolvedMap.value[src]) {
      return resolvedMap.value[src]
    }

    if (!pendingMap.value[src]) {
      // 商品接口返回的是文件对象名，这里统一换成文件服务给出的可访问地址。
      pendingMap.value[src] = api
        .get<any>(`/files/${encodeURIComponent(src)}/url`)
        .then((res) => {
          const resolved = res?.code === 200 && typeof res?.data === 'string' && res.data
            ? res.data
            : '/placeholder.svg'
          resolvedMap.value = { ...resolvedMap.value, [src]: resolved }
          return resolved
        })
        .catch(() => {
          resolvedMap.value = { ...resolvedMap.value, [src]: '/placeholder.svg' }
          return '/placeholder.svg'
        })
        .finally(() => {
          const next = { ...pendingMap.value }
          delete next[src]
          pendingMap.value = next
        })
    }

    return pendingMap.value[src]
  }

  function useResolvedImage(src: MaybeRefOrGetter<string | null | undefined>) {
    const value = computed(() => toValue(src) || '')
    const resolved = ref('/placeholder.svg')

    // 绑定到响应式数据源，图片对象名变化后自动重新解析。
    watch(value, async (next) => {
      resolved.value = await resolveImageUrl(next)
    }, { immediate: true })

    return resolved
  }

  async function primeImageUrls(values: Array<string | null | undefined>) {
    // 列表页先批量预热，避免首屏出现一排裸对象名请求 404。
    const unique = [...new Set(values.filter((value): value is string => !!value))]
    await Promise.all(unique.map((value) => resolveImageUrl(value)))
  }

  return {
    resolveImageUrl,
    useResolvedImage,
    primeImageUrls,
  }
}
