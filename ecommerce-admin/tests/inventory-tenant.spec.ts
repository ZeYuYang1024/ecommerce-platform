import { test, expect } from '@playwright/test'

test('merchant inventory page should use merchant inventory endpoint', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  const merchantUrls = []
  const platformUrls = []

  await page.route('**/api/v1/admin/merchant/inventory*', async (route) => {
    merchantUrls.push(route.request().url())
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } }),
    })
  })

  await page.route('**/api/v1/admin/inventory*', async (route) => {
    platformUrls.push(route.request().url())
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } }),
    })
  })

  await page.goto('/merchant/inventory')

  await expect.poll(() => merchantUrls.length).toBeGreaterThan(0)
  expect(platformUrls).toHaveLength(0)
  await expect(page.locator('.section-label')).toHaveText('库存管理')
  await expect(page.getByText('暂无本店库存记录，可先创建商品或补充 SKU')).toBeVisible()
})
