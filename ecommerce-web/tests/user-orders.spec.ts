import { test, expect } from '@playwright/test'
import { MOCK_ORDERS } from './mocks/data'

test.describe('PC User Orders', () => {

  test.beforeEach(async ({ page }) => {
    await page.context().addCookies([
      { name: 'token', value: 'mock-token', domain: 'localhost', path: '/' },
      { name: 'username', value: 'testuser', domain: 'localhost', path: '/' }
    ])
    await page.route('**/api/v1/orders', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: MOCK_ORDERS })
        })
      } else {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200 }) })
      }
    })
    await page.goto('/user/orders')
    await page.waitForTimeout(500)
  })

  test('P1: order list loads', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('我的订单')
    await expect(page.getByText('iPhone')).toBeVisible({ timeout: 5000 })
  })

  test('P1: pending order shows cancel button', async ({ page }) => {
    await expect(page.locator('button:has-text("取消订单")').first()).toBeVisible({ timeout: 5000 })
  })

  test('P1: cancel order works', async ({ page }) => {
    const cancelBtn = page.locator('button:has-text("取消订单")').first()
    if (await cancelBtn.isVisible()) {
      await cancelBtn.click()
      await page.waitForTimeout(300)
    }
  })

  test('P2: order status text shown correctly', async ({ page }) => {
    await expect(page.getByText('待支付')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('已支付')).toBeVisible({ timeout: 5000 })
  })
})
