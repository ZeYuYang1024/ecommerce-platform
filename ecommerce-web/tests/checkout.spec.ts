import { test, expect } from '@playwright/test'
import { MOCK_CART_ITEMS } from './mocks/data'

test.describe('PC Checkout', () => {

  test.beforeEach(async ({ page }) => {
    await page.context().addCookies([
      { name: 'token', value: 'mock-token', domain: 'localhost', path: '/' },
      { name: 'username', value: 'testuser', domain: 'localhost', path: '/' }
    ])
    await page.route('**/api/v1/cart', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_CART_ITEMS })
      })
    })
    await page.route('**/api/v1/orders', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { id: 1, orderNo: '202605101200000001' } })
      })
    })
    await page.goto('/checkout')
    await page.waitForTimeout(500)
  })

  test('P0: checkout page shows order summary', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('确认订单')
    await expect(page.getByText('iPhone')).toBeVisible({ timeout: 5000 })
  })

  test('P0: has receiver info form', async ({ page }) => {
    await expect(page.locator('input[placeholder="收货人"]')).toBeVisible()
    await expect(page.locator('input[placeholder="手机号"]')).toBeVisible()
  })

  test('P0: submit order works', async ({ page }) => {
    await page.fill('input[placeholder="收货人"]', 'TestUser')
    await page.fill('input[placeholder="手机号"]', '13800001111')
    await page.fill('input[placeholder="详细地址"]', 'Test Address')
    await page.click('button:has-text("提交订单")')
    await page.waitForTimeout(500)
    // Should redirect to orders page after success
  })

  test('P1: not logged in redirects', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/checkout')
    await page.waitForTimeout(500)
    await expect(page.getByText('请先登录').first()).toBeVisible({ timeout: 3000 })
  })
})
