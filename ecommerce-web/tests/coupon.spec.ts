import { expect, test } from '@playwright/test'

test('signed-in shoppers can claim a public coupon and see it in their personal list', async ({ page, baseURL }) => {
  await page.context().addCookies([{ name: 'token', value: 'mock-token', url: baseURL! }])

  const coupon = { id: '14', name: '测试优惠券', type: 'FLAT', discountAmount: 5 }
  let claimed = false

  await page.route('**/api/v1/cart', route => route.fulfill({ json: { code: 200, data: [] } }))
  await page.route('**/api/v1/coupons/available?*', route => route.fulfill({
    json: { code: 200, data: { records: [coupon], total: 1 } },
  }))
  await page.route('**/api/v1/coupons/mine?*', route => route.fulfill({
    json: {
      code: 200,
      data: { records: claimed ? [{ ...coupon, userCouponId: '29', status: 0 }] : [], total: Number(claimed) },
    },
  }))
  await page.route('**/api/v1/coupons/claim?templateId=14', route => {
    claimed = true
    return route.fulfill({ json: { code: 200 } })
  })
  page.on('dialog', dialog => dialog.accept())

  await page.goto('/coupon')
  const available = page.locator('section').filter({ has: page.getByRole('heading', { name: '可领取优惠券' }) })
  const mine = page.locator('section').filter({ has: page.getByRole('heading', { name: '我的优惠券' }) })

  await expect(mine.getByText('暂无优惠券')).toBeVisible()
  await available.getByRole('button', { name: '立即领取' }).click()
  await expect(mine.getByText(coupon.name, { exact: true })).toBeVisible()
  await expect(available.getByText(coupon.name, { exact: true })).toBeVisible()
})
