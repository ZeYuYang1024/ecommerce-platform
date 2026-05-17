import { test, expect } from '@playwright/test'

test('merchant knowledge page should render merchant documents from merchant endpoints', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  let documentEndpointHit = false
  let categoryEndpointHit = false

  await page.route('**/api/v1/admin/merchant/knowledge/documents*', async (route) => {
    documentEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        data: {
          records: [
            {
              id: 1,
              title: '商家发货 FAQ',
              categoryId: 9,
              categoryName: '发货',
              status: 'published',
              chunkCount: 2,
              updateTime: '2026-05-16 10:00:00'
            }
          ],
          total: 1,
          current: 1,
          size: 20
        }
      })
    })
  })

  await page.route('**/api/v1/admin/merchant/knowledge/categories*', async (route) => {
    categoryEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: [{ id: 9, name: '发货' }] })
    })
  })

  await page.goto('/merchant/knowledge')

  await expect.poll(() => documentEndpointHit).toBeTruthy()
  await expect.poll(() => categoryEndpointHit).toBeTruthy()
  await expect(page.getByText('商家发货 FAQ')).toBeVisible()
  await expect(page.getByText('发货', { exact: true }).first()).toBeVisible()
})

test('merchant knowledge page should show merchant empty state copy', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  await page.route('**/api/v1/admin/merchant/knowledge/documents*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 20 } })
    })
  })

  await page.route('**/api/v1/admin/merchant/knowledge/categories*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: [] })
    })
  })

  await page.goto('/merchant/knowledge')

  await expect(page.getByText('暂无知识文档，新建后即可用于商家问答')).toBeVisible()
})
