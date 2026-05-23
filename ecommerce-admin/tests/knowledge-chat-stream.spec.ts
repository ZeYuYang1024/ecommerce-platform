import { test, expect } from '@playwright/test'

function sse(event: string, data: unknown) {
  return `event:${event}\ndata:${typeof data === 'string' ? data : JSON.stringify(data)}\n\n`
}

test('platform knowledge chat page should stream from admin knowledge endpoint', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'ops')
    localStorage.setItem('username', 'ops-a')
  })

  let streamEndpointHit = false

  await page.route('**/api/v1/admin/knowledge/chat/stream', async (route) => {
    streamEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream; charset=utf-8',
      body: [
        sse('start', 'admin-session'),
        sse('lightRoute', 'RAG_FAQ_CHANNEL'),
        sse('route', 'RAG_FAQ'),
        sse('chunk', '平台'),
        sse('chunk', '知识库'),
        sse('answer', { answer: '平台知识库', sessionId: 'admin-session', sources: [] }),
        sse('done', 'admin-session'),
      ].join(''),
    })
  })

  await page.goto('/knowledge/chat')
  await page.getByPlaceholder('输入问题或调试语句').fill('平台规则是什么')
  await page.getByRole('button', { name: '发送' }).click()

  await expect.poll(() => streamEndpointHit).toBeTruthy()
  await expect(page.locator('.conversation-panel').getByText('平台知识库', { exact: true })).toBeVisible()
  await expect(page.getByText('轻量路由')).toBeVisible()
  await expect(page.getByText('RAG_FAQ_CHANNEL', { exact: true })).toBeVisible()
  await expect(page.getByText('业务路由')).toBeVisible()
  await expect(page.getByText('RAG_FAQ', { exact: true })).toBeVisible()
  await expect(page.getByText('最终答案')).toBeVisible()
  await expect(page.getByText('{"answer":"平台知识库","sessionId')).toHaveCount(0)
})

test('merchant knowledge chat page should stream from merchant knowledge endpoint', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
    localStorage.setItem('merchantId', '9001')
  })

  let streamEndpointHit = false

  await page.route('**/api/v1/admin/merchant/knowledge/chat/stream', async (route) => {
    streamEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream; charset=utf-8',
      body: [
        sse('start', 'merchant-session'),
        sse('lightRoute', 'RAG_FAQ_CHANNEL'),
        sse('route', 'RAG_FAQ'),
        sse('chunk', '商家'),
        sse('chunk', '发货'),
        sse('answer', { answer: '商家发货', sessionId: 'merchant-session', sources: [] }),
        sse('done', 'merchant-session'),
      ].join(''),
    })
  })

  await page.goto('/merchant/knowledge/chat')
  await page.getByPlaceholder('输入问题或调试语句').fill('商家发货规则是什么')
  await page.getByRole('button', { name: '发送' }).click()

  await expect.poll(() => streamEndpointHit).toBeTruthy()
  await expect(page.locator('.conversation-panel').getByText('商家发货', { exact: true })).toBeVisible()
})

test('platform knowledge chat page should show login expired message on 401 stream response', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'ops')
    localStorage.setItem('username', 'ops-a')
  })

  await page.route('**/api/v1/admin/knowledge/chat/stream', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ code: 401, message: 'Unauthorized' }),
    })
  })

  await page.goto('/knowledge/chat')
  await page.getByPlaceholder('输入问题或调试语句').fill('平台规则是什么')
  await page.getByRole('button', { name: '发送' }).click()

  await expect(page.getByText('登录已失效，请重新登录')).toBeVisible()
})
