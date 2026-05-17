import { test, expect } from '@playwright/test'

const DASHBOARD_STATS = {
  merchantCount: 128,
  pendingAuditCount: 7,
  productCount: 256,
  userCount: 1024,
}

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
      localStorage.setItem('type', 'super_admin')
    })

    await page.route('**/api/v1/admin/dashboard/stats', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: DASHBOARD_STATS }),
      })
    })

    await page.goto('/dashboard')
  })

  test('P1: dashboard loads with 4 stat cards', async ({ page }) => {
    await expect(page.locator('.stat-card')).toHaveCount(4)
  })

  test('P1: welcome message shows username', async ({ page }) => {
    await expect(page.locator('.greeting h1')).toContainText('admin')
  })

  test('P1: stat cards show mocked values', async ({ page }) => {
    const values = page.locator('.stat-value')
    await expect(values).toHaveCount(4)
    await expect(values.nth(0)).toContainText('128')
    await expect(values.nth(1)).toContainText('7')
    await expect(values.nth(2)).toContainText('256')
    await expect(values.nth(3)).toContainText('1024')
  })

  test('P1: merchant audit quick link navigates to pending merchants', async ({ page }) => {
    await page.locator('.quick-links .el-button').nth(0).click()
    await page.waitForURL('**/merchants?status=0')
  })

  test('P1: category quick link navigates to categories', async ({ page }) => {
    await page.locator('.quick-links .el-button').nth(1).click()
    await page.waitForURL('**/categories')
  })
})
