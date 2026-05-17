import { test, expect } from '@playwright/test'

test('merchant knowledge page should treat code 0 as success for knowledge responses', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  await page.route('**/api/v1/admin/merchant/knowledge/documents*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          records: [
            {
              id: 101,
              title: 'Merchant Shipping Policy',
              categoryId: 9,
              categoryName: 'Merchant FAQ',
              status: 'published',
              chunkCount: 2,
              updateTime: '2026-05-16 23:53:00'
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
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: [{ id: 9, name: 'Merchant FAQ' }] })
    })
  })

  await page.goto('/merchant/knowledge')
  await page.waitForTimeout(300)

  await expect(page.getByText('Merchant Shipping Policy')).toBeVisible()
  await expect(page.getByText('Merchant FAQ', { exact: true }).first()).toBeVisible()
})
