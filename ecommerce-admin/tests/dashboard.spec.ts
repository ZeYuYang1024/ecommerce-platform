import { test, expect } from '@playwright/test'

test.describe('Dashboard', () => {

  test.beforeEach(async ({ page }) => {
    // Set token so router guard lets us through
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })
    await page.goto('/dashboard')
  })

  // ---- P1: Core features ----

  test('P1: dashboard loads with 4 stat cards', async ({ page }) => {
    await expect(page.locator('.stat-card')).toHaveCount(4)
  })

  test('P1: welcome message shows username', async ({ page }) => {
    await expect(page.locator('.greeting h1')).toContainText('admin')
  })

  test('P1: stat cards show placeholder values', async ({ page }) => {
    // All cards show "--" since no backend integration
    const values = page.locator('.stat-value')
    await expect(values).toHaveCount(4)
    // at least one should show "--"
    await expect(values.first()).toContainText('--')
  })

  test('P1: "新增商品" quick link navigates to create page', async ({ page }) => {
    await page.click('button:has-text("新增商品")')
    await page.waitForURL('**/products/create')
  })

  test('P1: "商品列表" quick link navigates to product list', async ({ page }) => {
    await page.click('button:has-text("商品列表")')
    await page.waitForURL('**/products')
  })
})
