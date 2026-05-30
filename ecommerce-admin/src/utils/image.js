import axios from 'axios'

const resolvedImageMap = new Map()
const pendingImageMap = new Map()

export async function resolveImageUrl(src) {
  if (!src) return ''
  if (src.startsWith('http') || src.startsWith('/')) return src

  if (resolvedImageMap.has(src)) {
    return resolvedImageMap.get(src)
  }

  if (!pendingImageMap.has(src)) {
    pendingImageMap.set(
      src,
      axios.get(`/api/v1/files/${encodeURIComponent(src)}/url`)
        .then(({ data }) => {
          const resolved = data?.code === 200 && typeof data?.data === 'string' ? data.data : ''
          resolvedImageMap.set(src, resolved)
          return resolved
        })
        .catch(() => {
          resolvedImageMap.set(src, '')
          return ''
        })
        .finally(() => {
          pendingImageMap.delete(src)
        })
    )
  }

  return pendingImageMap.get(src)
}

export async function resolveImageUrls(values) {
  const unique = [...new Set((values || []).filter(Boolean))]
  const entries = await Promise.all(unique.map(async (value) => [value, await resolveImageUrl(value)]))
  return Object.fromEntries(entries)
}
