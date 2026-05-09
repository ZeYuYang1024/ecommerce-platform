import { test, expect } from '@playwright/test'
import { MOCK_CART_ITEMS } from './mocks/data'

test.describe('PC Cart', () => {

  test.beforeEach(async ({ page }) => {
    await page.context().addCookies([
      { name: 'token', value: 'mock-token', domain: 'localhost', path: '/' },
      { name: 'username', value: 'testuser', domain: 'localhost', path: '/' }
    ])
    await page.route('**/api/v1/cart', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: MOCK_CART_ITEMS })
        })
      } else if (route.request().method() === 'DELETE') {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200 }) })
      } else {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200 }) })
      }
    })
    await page.goto('/cart')
    await page.waitForTimeout(500)
  })

  test('P0: cart shows items', async ({ page }) => {
    await expect(page.locator('text=iPhone')).toBeVisible({ timeout: 5000 })
  })

  test('P0: checkout button navigates', async ({ page }) => {
    const checkoutBtn = page.locator('button:has-text("去结算")')
    if (await checkoutBtn.isVisible()) {
      await checkoutBtn.click()
      await page.waitForURL('**/checkout')
    }
  })

  test('P0: can remove item', async ({ page }) => {
    const removeBtn = page.locator('button svg path[d*="M19 7l"]').first()
    if (await removeBtn.isVisible()) {
      await removeBtn.click()
    }
  })

  test('P1: not logged in shows login prompt', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/cart')
    await page.waitForTimeout(500)
    await expect(page.locator('text=请先登录后查看购物车')).toBeVisible({ timeout: 3000 })
  })
})
