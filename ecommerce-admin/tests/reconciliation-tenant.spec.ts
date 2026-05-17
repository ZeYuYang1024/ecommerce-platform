import { test, expect } from '@playwright/test'

test('merchant reconciliation page should use merchant reconciliation endpoint', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  let merchantEndpointHit = false
  let platformEndpointHit = false

  await page.route('**/api/**', async (route) => {
    const url = route.request().url()

    if (url.includes('/api/v1/admin/merchant/reconciliation')) {
      merchantEndpointHit = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0, current: 1, size: 10 },
        }),
      })
      return
    }

    if (url.includes('/api/v1/admin/reconciliation')) {
      platformEndpointHit = true
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } }),
    })
  })

  await page.goto('/merchant/reconciliation')

  await expect.poll(() => merchantEndpointHit).toBeTruthy()
  expect(platformEndpointHit).toBeFalsy()
  await expect(page.getByRole('button', { name: '执行对账' })).toHaveCount(0)
  await expect(page.getByText('暂无本店对账结果')).toBeVisible()
})

test('merchant reconciliation detail should stay on merchant route', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  await page.route('**/api/**', async (route) => {
    const url = route.request().url()

    if (url.includes('/api/v1/admin/merchant/reconciliation/1')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 1,
            batchNo: 'REC001',
            totalOrderCount: 1,
            totalPaymentCount: 1,
            matchedCount: 1,
            unmatchedCount: 0,
            status: 1,
            statusText: '已完成',
            details: [],
          },
        }),
      })
      return
    }

    if (url.includes('/api/v1/admin/merchant/reconciliation')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 1,
                batchNo: 'REC001',
                totalOrderCount: 1,
                totalPaymentCount: 1,
                matchedCount: 1,
                unmatchedCount: 0,
                status: 1,
                statusText: '已完成',
                createdAt: '2026-05-16T10:00:00',
              },
            ],
            total: 1,
            current: 1,
            size: 10,
          },
        }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: {} }),
    })
  })

  await page.goto('/merchant/reconciliation')
  await page.getByRole('button', { name: '查看明细' }).click()

  await expect(page).toHaveURL(/\/merchant\/reconciliation\/1$/)
  await expect(page.locator('.summary-card')).toBeVisible()
})

test('merchant reconciliation detail should show an explicit unavailable state when detail is forbidden', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  await page.route('**/api/**', async (route) => {
    const url = route.request().url()

    if (url.includes('/api/v1/admin/merchant/reconciliation/99')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 50010006,
          message: '对账记录不存在',
        }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: {} }),
    })
  })

  await page.goto('/merchant/reconciliation/99')

  await expect(page).toHaveURL(/\/merchant\/reconciliation\/99$/)
  await expect(page.locator('.detail-empty')).toContainText('对账记录不存在')
  await expect(page.locator('.summary-card')).toHaveCount(0)
})
