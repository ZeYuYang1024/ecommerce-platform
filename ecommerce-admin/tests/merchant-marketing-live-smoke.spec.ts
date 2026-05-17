import { expect, test } from '@playwright/test'

test('merchant live smoke for coupon and seckill tenant pages', async ({ page }) => {
  test.setTimeout(60000)

  await page.goto('/login')
  await page.locator('.login-form input').nth(0).fill('m_2053170012063142000')
  await page.locator('.login-form input').nth(1).fill('admin123')
  await page.locator('.submit-btn').click()

  await page.waitForURL(/merchant\/dashboard|dashboard/, { timeout: 15000 })

  const couponResponsePromise = page.waitForResponse(
    response => response.url().includes('/api/v1/admin/merchant/coupons') && response.request().method() === 'GET',
    { timeout: 10000 }
  )

  await page.goto('/merchant/coupons')
  await page.waitForLoadState('networkidle')
  const couponResponse = await couponResponsePromise
  expect(couponResponse.status()).toBe(200)
  const couponBody = JSON.parse(await couponResponse.text())
  expect(couponBody.code).toBe(200)
  const couponNames = (couponBody.data?.records || []).map((record: any) => record.name)
  expect(couponNames).toEqual(expect.arrayContaining([
    'merchant-live-coupon-A-20260517',
    'merchant-live-coupon-B-20260517'
  ]))
  expect(couponNames).not.toContain('noise-merchant-coupon-20260517')
  await expect(page.locator('body')).toContainText('merchant-live-coupon-A-20260517')
  await expect(page.locator('body')).toContainText('merchant-live-coupon-B-20260517')
  await expect(page.locator('body')).not.toContainText('noise-merchant-coupon-20260517')

  const sessionResponsePromise = page.waitForResponse(
    response => response.url().includes('/api/v1/admin/merchant/seckill/sessions') && response.request().method() === 'GET',
    { timeout: 10000 }
  )
  const itemResponsePromise = page.waitForResponse(
    response => response.url().includes('/api/v1/admin/merchant/seckill/items') && response.request().method() === 'GET',
    { timeout: 10000 }
  )

  await page.goto('/merchant/seckill')
  await page.waitForLoadState('networkidle')
  const sessionResponse = await sessionResponsePromise
  const itemResponse = await itemResponsePromise

  expect(sessionResponse.status()).toBe(200)
  expect(itemResponse.status()).toBe(200)

  const sessionBody = JSON.parse(await sessionResponse.text())
  const itemBody = JSON.parse(await itemResponse.text())
  expect(sessionBody.code).toBe(200)
  expect(itemBody.code).toBe(200)

  const sessionNames = (sessionBody.data?.records || []).map((record: any) => record.name)
  const itemNames = (itemBody.data?.records || []).map((record: any) => record.name)
  expect(sessionNames).toContain('merchant-live-session-20260517')
  expect(sessionNames).not.toContain('noise-session-20260517')
  expect(itemNames).toEqual(expect.arrayContaining([
    'merchant-live-seckill-smoke-standard',
    'merchant-live-seckill-seed-standard'
  ]))
  expect(itemNames).not.toContain('noise-seckill-item-20260517')

  await expect(page.locator('body')).toContainText('merchant-live-session-20260517')
  await page.getByRole('tab').nth(1).click()
  await expect(page.locator('body')).toContainText('merchant-live-seckill-smoke-standard')
  await expect(page.locator('body')).toContainText('merchant-live-seckill-seed-standard')
  await expect(page.locator('body')).not.toContainText('noise-seckill-item-20260517')
})
