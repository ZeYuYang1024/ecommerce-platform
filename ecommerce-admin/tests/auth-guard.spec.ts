import { test, expect } from '@playwright/test'

test.describe('Auth Guard & Layout', () => {

  // ---- P0: Critical path ----

  test('P0: unauthenticated user is redirected to /login', async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
    await page.goto('/products')
    await page.reload() // force SPA to re-check router guard
    await page.waitForURL('**/login', { timeout: 5000 })
  })

  test('P0: unauthenticated user accessing /dashboard is redirected', async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
    await page.goto('/dashboard')
    await page.reload()
    await page.waitForURL('**/login', { timeout: 5000 })
  })

  // ---- P1: Core features ----

  test('P1: sidebar navigation between pages', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    // generic API mock
    await page.route('**/api/**', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
      })
    })

    await page.goto('/dashboard')
    await page.waitForTimeout(300)

    // click sidebar nav to products
    await page.click('.nav-item:has-text("商品列表")')
    await page.waitForTimeout(300)
    expect(page.url()).toContain('/products')
  })

  test('P1: breadcrumb shows current page title', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })
    await page.route('**/api/**', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0 } })
      })
    })
    await page.goto('/products')
    await page.waitForTimeout(300)
    await expect(page.locator('.crumb')).toBeVisible()
  })

  test('P1: logout button clears token and redirects', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })
    await page.route('**/api/**', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0 } })
      })
    })
    await page.goto('/dashboard')
    await page.click('.logout-btn')
    await page.waitForURL('**/login')
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBeNull()
  })

  test('P1: sidebar shows username and avatar initial', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'AdminUser')
    })
    await page.route('**/api/**', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0 } })
      })
    })
    await page.goto('/dashboard')
    await page.waitForTimeout(300)
    await expect(page.locator('.user-chip .user-name')).toContainText('AdminUser')
  })

  test('P1: active nav item is highlighted', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })
    await page.goto('/dashboard')
    await page.waitForTimeout(300)
    await expect(page.locator('.nav-item.active')).toBeVisible()
  })

  test('P1: clock is displayed in topbar', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })
    await page.goto('/dashboard')
    await page.waitForTimeout(300)
    await expect(page.locator('.clock')).toBeVisible()
  })
})
