import { expect, test } from '@playwright/test'

test('merchant live smoke for products inventory and shop', async ({ page }) => {
  test.setTimeout(60000)

  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill('m_2053170012063142000')
  await page.getByPlaceholder('请输入密码').fill('admin123')
  await page.locator('.submit-btn').click()

  await page.waitForURL(/merchant\/dashboard|dashboard/, { timeout: 15000 })

  await page.goto('/merchant/products')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)
  await expect(page.locator('body')).toContainText('merchant-live-smoke-')
  await expect(page.locator('body')).toContainText('merchant-live-seed-')

  await page.goto('/merchant/inventory')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)
  await expect(page.locator('body')).toContainText('smoke-standard')
  await expect(page.locator('body')).toContainText('seed-standard')
  await expect(page.locator('body')).toContainText('42')

  const merchantDetailPromise = page.waitForResponse(
    response => response.url().includes('/api/v1/admin/merchants/') && response.request().method() === 'GET',
    { timeout: 10000 }
  ).catch(() => null)

  await page.goto('/merchant/shop')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)
  const merchantDetailResponse = await merchantDetailPromise
  expect(merchantDetailResponse).not.toBeNull()
  const merchantDetailBody = JSON.parse(await merchantDetailResponse.text())
  expect(merchantDetailBody.code).toBe(200)
  await expect(page.locator('body')).toContainText('E2E_Shop_1778406423007')
})
