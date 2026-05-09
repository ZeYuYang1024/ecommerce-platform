import { test, expect } from '@playwright/test'

test.describe('PC Login', () => {

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/auth/login', async (route, req) => {
      const body = JSON.parse(req.postData() || '{}')
      if (body.username === 'testuser' && body.password === '123456') {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: { token: 'mock-token', userId: 1, username: 'testuser' } })
        })
      } else {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ code: 10001003, message: '密码错误' })
        })
      }
    })
    await page.goto('/login')
  })

  test('P1: login success redirects to home', async ({ page }) => {
    await page.fill('input[placeholder="用户名"]', 'testuser')
    await page.fill('input[placeholder="密码"]', '123456')
    await page.click('button:has-text("登 录")')
    await page.waitForTimeout(1000)
    expect(page.url()).not.toContain('/login')
  })

  test('P1: login shows register link', async ({ page }) => {
    await expect(page.locator('a:has-text("立即注册")')).toBeVisible()
  })

  test('P1: register link navigates', async ({ page }) => {
    await page.click('a:has-text("立即注册")')
    await page.waitForURL('**/register')
  })
})
