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
    const res = await $fetch<T>(`${base}${path}`, { ...options, headers })
    return res
  }

  return {
    get: <T>(path: string) => request<T>(path),
    post: <T>(path: string, body?: any) => request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
    put: <T>(path: string, body?: any) => request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
    delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  }
}
