import { test, expect } from '@playwright/test'
import { MOCK_CART_ITEMS } from './mocks/data'

const MOCK_ADDRESSES = [
  {
    id: 1,
    receiverName: 'TestUser',
    receiverPhone: '13800001111',
    province: '北京市',
    city: '北京市',
    district: '朝阳区',
    detail: '测试地址 1 号',
    isDefault: true
  }
]

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
    await page.route('**/api/v1/users/addresses**', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: MOCK_ADDRESSES })
        })
      } else {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: { ...MOCK_ADDRESSES[0], id: 2, isDefault: false } })
        })
      }
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

  test('P0: shows default receiver address', async ({ page }) => {
    await expect(page.getByText('收货地址')).toBeVisible()
    await expect(page.locator('.bg-amber-50').getByText('TestUser', { exact: true })).toBeVisible()
    await expect(page.locator('.bg-amber-50').getByText('13800001111')).toBeVisible()
  })

  test('P0: submit order works', async ({ page }) => {
    await page.click('button:has-text("提交订单")')
    await page.waitForURL('**/payment/202605101200000001')
  })

  test('P1: not logged in redirects', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/checkout')
    await page.waitForTimeout(500)
    await expect(page.getByText('请先登录').first()).toBeVisible({ timeout: 3000 })
  })
})
