const MAX_SAFE = 9007199254740991

// JSON reviver: convert large integers to strings (Snowflake IDs)
function parseBigInt(text: string): any {
  return JSON.parse(text, (key: string, value: any) => {
    if (typeof value === 'number' && Number.isInteger(value) && (value > MAX_SAFE || value < -MAX_SAFE)) {
      return String(value)
    }
    return value
  })
}

export const useApi = () => {
  const config = useRuntimeConfig()
  const base = config.public.apiBase as string
  const token = useCookie('token')

  async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string> || {}),
    }
    if (token.value) {
      headers['Authorization'] = `Bearer ${token.value}`
    }
    const res = await fetch(`${base}${path}`, { ...options, headers })
    const text = await res.text()
    return parseBigInt(text)
  }

  return {
    get: <T>(path: string, params?: Record<string, any>) => {
      let url = path
      if (params) {
        const qs = Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== null)
          .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
          .join('&')
        if (qs) url += (path.includes('?') ? '&' : '?') + qs
      }
      return request<T>(url)
    },
    post: <T>(path: string, body?: any) => request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
    put: <T>(path: string, body?: any) => request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
    delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  }
}
