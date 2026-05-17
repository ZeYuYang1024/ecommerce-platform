import { test, expect } from '@playwright/test'

function seedMerchantSession(page) {
  return page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
    localStorage.setItem('merchantId', '2001')
  })
}

test.describe('Merchant formatting follow-ups', () => {
  test('merchant payments should format paidAt without ISO T separator', async ({ page }) => {
    await seedMerchantSession(page)

    await page.route('**/api/v1/admin/merchant/payment*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [{
              paymentNo: 'PAY-001',
              orderNo: 'ORD-001',
              userId: 9001,
              amount: '88.00',
              payMethod: 'ALIPAY',
              status: 1,
              statusText: '已支付',
              paidAt: '2026-05-16T23:55:00'
            }],
            total: 1,
            current: 1,
            size: 10
          }
        })
      })
    })

    await page.goto('/merchant/payments')

    await expect(page.getByText('2026-05-16 23:55:00')).toBeVisible()
    await expect(page.getByText('2026-05-16T23:55:00')).toHaveCount(0)
  })

  test('merchant orders should format createdAt without ISO T separator', async ({ page }) => {
    await seedMerchantSession(page)

    await page.route('**/api/v1/admin/merchant/orders*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [{
              orderNo: 'ORD-1001',
              totalAmount: '128.00',
              status: 1,
              statusText: '已支付',
              receiverName: 'Merchant Buyer',
              createdAt: '2026-05-16T21:30:45'
            }],
            total: 1,
            current: 1,
            size: 10
          }
        })
      })
    })

    await page.goto('/merchant/orders')

    await expect(page.getByText('2026-05-16 21:30:45')).toBeVisible()
    await expect(page.getByText('2026-05-16T21:30:45')).toHaveCount(0)
  })

  test('merchant shop should format createdAt without ISO T separator', async ({ page }) => {
    await seedMerchantSession(page)

    await page.route('**/api/v1/admin/merchants/2001', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 2001,
            name: 'Shop Demo',
            contactName: 'Owner',
            contactPhone: '13800000000',
            businessLicense: 'https://cdn.example.com/license.png',
            logo: 'https://cdn.example.com/logo.png',
            status: 1,
            statusText: '已通过',
            createdAt: '2026-05-16T20:00:00'
          }
        })
      })
    })

    await page.goto('/merchant/shop')

    await expect(page.getByText('2026-05-16 20:00:00')).toBeVisible()
    await expect(page.getByText('2026-05-16T20:00:00')).toHaveCount(0)
  })

  test('merchant reviews should format createdAt without ISO T separator', async ({ page }) => {
    await seedMerchantSession(page)

    await page.route('**/api/v1/admin/merchant/reviews*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [{
              id: 7001,
              content: '很不错',
              spuId: 88,
              username: 'buyer-a',
              rating: 5,
              createdAt: '2026-05-16T19:10:00'
            }],
            total: 1,
            current: 1,
            size: 10
          }
        })
      })
    })

    await page.goto('/merchant/reviews')

    await expect(page.getByText('2026-05-16 19:10:00')).toBeVisible()
    await expect(page.getByText('2026-05-16T19:10:00')).toHaveCount(0)
  })

  test('merchant knowledge should format updateTime without ISO T separator', async ({ page }) => {
    await seedMerchantSession(page)

    await page.route('**/api/v1/admin/merchant/knowledge/documents*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [{
              id: 1,
              title: 'Merchant FAQ',
              categoryId: 9,
              categoryName: '物流',
              status: 'published',
              chunkCount: 2,
              updateTime: '2026-05-16T18:22:11'
            }],
            total: 1,
            current: 1,
            size: 20
          }
        })
      })
    })

    await page.route('**/api/v1/admin/merchant/knowledge/categories*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [{ id: 9, name: '物流' }] })
      })
    })

    await page.goto('/merchant/knowledge')

    await expect(page.getByText('2026-05-16 18:22:11')).toBeVisible()
    await expect(page.getByText('2026-05-16T18:22:11')).toHaveCount(0)
  })

  test('merchant brands should render a formatted update time column', async ({ page }) => {
    await seedMerchantSession(page)

    await page.route('**/api/v1/admin/merchant/brands*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [{
              id: 301,
              name: 'Merchant Brand',
              logo: '',
              description: 'Merchant owned brand',
              auditStatus: 'approved',
              updatedAt: '2026-05-16T17:00:00',
              createdAt: '2026-05-15T08:00:00'
            }],
            total: 1,
            current: 1,
            size: 10
          }
        })
      })
    })

    await page.goto('/merchant/brands')

    await expect(page.getByText('更新时间')).toBeVisible()
    await expect(page.getByText('2026-05-16 17:00:00')).toBeVisible()
    await expect(page.getByText('2026-05-16T17:00:00')).toHaveCount(0)
  })

  test('merchant settlement should keep merchant table content visible without platform action button', async ({ page }) => {
    await seedMerchantSession(page)

    await page.route('**/api/v1/admin/merchant/settlement*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [{
              settlementDate: '2026-05-16T00:00:00',
              totalPaymentCount: 12,
              totalPaymentAmount: '2568.00',
              totalRefundCount: 1,
              totalRefundAmount: '88.00',
              netAmount: '2480.00',
              status: 1,
              statusText: '已完成'
            }],
            total: 1,
            current: 1,
            size: 10
          }
        })
      })
    })

    await page.goto('/merchant/settlement')

    await expect(page.getByRole('button', { name: '生成今日结算' })).toHaveCount(0)
    await expect(page.getByText('2026-05-16 00:00:00')).toBeVisible()
    await expect(page.getByText('2480.00')).toBeVisible()
  })
})
