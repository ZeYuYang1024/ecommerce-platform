import { expect, test } from '@playwright/test'

test('merchant live smoke should show unavailable state for foreign reconciliation detail', async ({ page, request }) => {
  test.setTimeout(60000)

  const loginResponse = await request.post('http://localhost:8080/api/v1/auth/admin/login', {
    data: {
      username: 'm_2053170012063142000',
      password: 'admin123',
    },
  })

  expect(loginResponse.ok()).toBeTruthy()
  const loginBody = await loginResponse.json()
  expect(loginBody.code).toBe(200)

  await page.goto('/login')
  await page.evaluate((auth) => {
    localStorage.setItem('token', auth.token)
    localStorage.setItem('username', auth.username)
    localStorage.setItem('type', auth.type || 'merchant')
    localStorage.setItem('merchantId', String(auth.merchantId))
  }, loginBody.data)

  const detailResponsePromise = page.waitForResponse(
    response => response.url().includes('/api/v1/admin/merchant/reconciliation/3053170012063142601')
      && response.request().method() === 'GET',
    { timeout: 15000 }
  )

  await page.goto('/merchant/reconciliation/3053170012063142601')

  const detailResponse = await detailResponsePromise
  const detailBody = await detailResponse.json()

  expect(detailBody.code).toBe(50010006)
  await expect(page).toHaveURL(/\/merchant\/reconciliation\/3053170012063142601$/)
  await expect(page.locator('.detail-empty')).toBeVisible()
  await expect(page.locator('.detail-empty-title')).toBeVisible()
  await expect(page.locator('.detail-empty-text')).toBeVisible()
  await expect(page.locator('.summary-card')).toHaveCount(0)
})
