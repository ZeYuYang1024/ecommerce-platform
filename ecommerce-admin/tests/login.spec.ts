import { test, expect } from '@playwright/test'

test.describe('Login Page', () => {

  test.beforeEach(async ({ page }) => {
    // intercept admin login API
    await page.route('**/api/v1/auth/admin/login', async (route, req) => {
      const body = JSON.parse(req.postData() || '{}')
      if (body.username === 'admin' && body.password === 'admin123') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200, message: 'success',
            data: { token: 'mock-jwt-token', userId: 1, username: 'admin' }
          })
        })
      } else if (body.username === 'admin' && body.password !== 'admin123') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 10100002, message: '管理员密码错误' })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 10100001, message: '管理员不存在' })
        })
      }
    })
    await page.goto('/login')
  })

  // ---- P0: Critical path ----

  test('P0: successful admin login redirects to dashboard', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')

    await page.waitForURL('**/dashboard')
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBe('mock-jwt-token')
  })

  test('P0: empty form prevents submission and stays on login', async ({ page }) => {
    await page.click('button:has-text("登 录")')
    await page.waitForTimeout(800)
    // Should NOT navigate away - still on login page
    expect(page.url()).toContain('/login')
    // The form should not have submitted successfully (no dashboard redirect)
  })

  test('P0: only username filled prevents submission', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.click('button:has-text("登 录")')
    await page.waitForTimeout(800)
    // Still on login page, not redirected
    expect(page.url()).toContain('/login')
  })

  test('P0: only password filled prevents submission', async ({ page }) => {
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForTimeout(800)
    // Still on login page, not redirected
    expect(page.url()).toContain('/login')
  })

  // ---- P1: Core features ----

  test('P1: wrong password shows API error message', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'wrongpassword')
    await page.click('button:has-text("登 录")')

    await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 })
  })

  test('P1: non-existent admin shows error', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', 'nonexistent')
    await page.fill('input[placeholder="请输入密码"]', 'whatever')
    await page.click('button:has-text("登 录")')

    await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 })
  })

  test('P1: login button shows loading state', async ({ page }) => {
    // delay the response to see loading
    await page.route('**/api/v1/auth/admin/login', async (route) => {
      await new Promise(r => setTimeout(r, 1500))
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200, message: 'success',
          data: { token: 'mock-token', userId: 1, username: 'admin' }
        })
      })
    }, { times: 1 })

    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')

    // button should have loading class
    await expect(page.locator('button.is-loading')).toBeVisible({ timeout: 1000 })
  })

  test('P1: already logged in user can access /login without redirect', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('token', 'existing-token'))
    await page.goto('/login')
    // Should still be on login page (meta.noAuth = true)
    await expect(page.locator('h1')).toContainText('MERCH')
  })

  test('P1: password visibility toggle works', async ({ page }) => {
    const pwdInput = page.locator('input[placeholder="请输入密码"]')
    await expect(pwdInput).toHaveAttribute('type', 'password')

    // click the custom toggle icon
    await page.click('.pwd-toggle')
    await page.waitForTimeout(300)
    await expect(pwdInput).toHaveAttribute('type', 'text')

    // toggle back
    await page.click('.pwd-toggle')
    await page.waitForTimeout(300)
    await expect(pwdInput).toHaveAttribute('type', 'password')
  })
})
