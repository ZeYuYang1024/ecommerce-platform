import { postJsonSse } from '../../../frontend-shared/chatSse.mjs'

export async function postChatStream(url, payload, onEvent) {
  const headers = {
  }
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  await postJsonSse({
    url,
    payload,
    headers,
    onEvent,
  })
}
