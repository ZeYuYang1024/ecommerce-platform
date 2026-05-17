import { test, expect } from '@playwright/test'

function makeJwt(payload) {
  const encode = (value) => Buffer.from(JSON.stringify(value)).toString('base64url')
  return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode(payload)}.signature`
}

function makeJwtFromRawPayload(payloadJson) {
  const encode = (value) => Buffer.from(value).toString('base64url')
  return `${encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))}.${encode(payloadJson)}.signature`
}

test.describe('Merchant auth and shop follow-ups', () => {
  test('merchant login should persist merchantId and land on merchant dashboard', async ({ page }) => {
    await page.route('**/api/v1/admin/merchant/products*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { total: 0, records: [] } })
      })
    })

    await page.route('**/api/v1/admin/merchant/orders*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { total: 0, records: [] } })
      })
    })

    await page.route('**/api/v1/auth/admin/login', async (route, req) => {
      const body = JSON.parse(req.postData() || '{}')
      if (body.username === 'merchant-a' && body.password === 'merchant123') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            message: 'success',
            data: {
              token: 'merchant-token',
              userId: 2001,
              username: 'merchant-a',
              type: 'merchant',
              merchantId: 2001
            }
          })
        })
        return
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 10100001, message: 'login failed' })
      })
    })

    await page.goto('/login')
    await page.locator('.login-form input').nth(0).fill('merchant-a')
    await page.locator('.login-form input').nth(1).fill('merchant123')
    await page.locator('.submit-btn').click()

    await page.waitForURL('**/merchant/dashboard')
    const storage = await page.evaluate(() => ({
      token: localStorage.getItem('token'),
      username: localStorage.getItem('username'),
      type: localStorage.getItem('type'),
      merchantId: localStorage.getItem('merchantId')
    }))

    expect(storage).toEqual({
      token: 'merchant-token',
      username: 'merchant-a',
      type: 'merchant',
      merchantId: '2001'
    })
  })

  test('merchant shop page should load current shop and save updates', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'merchant-a')
      localStorage.setItem('type', 'merchant')
      localStorage.setItem('merchantId', '2001')
    })

    let currentMerchant = {
      id: 2001,
      name: 'Old Store',
      logo: 'https://cdn.example.com/old-logo.png',
      contactName: 'Old Owner',
      contactPhone: '13800000000',
      businessLicense: 'https://cdn.example.com/license-old.png',
      status: 1,
      statusText: '已通过',
      createdAt: '2026-05-17 09:00:00',
      reason: ''
    }

    let updatePayload = null

    await page.route('**/api/v1/admin/merchants/2001', async (route, request) => {
      if (request.method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: currentMerchant })
        })
        return
      }

      updatePayload = JSON.parse(request.postData() || '{}')
      currentMerchant = { ...currentMerchant, ...updatePayload }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: currentMerchant })
      })
    })

    await page.goto('/merchant/shop')
    await expect(page.locator('text=Old Store')).toBeVisible()

    await page.locator('[data-testid=\"shop-edit-button\"]').click()
    await page.getByPlaceholder('请输入店铺名称').fill('New Store')
    await page.getByPlaceholder('请输入联系人').fill('New Owner')
    await page.getByPlaceholder('请输入联系电话').fill('13911112222')
    await page.getByPlaceholder('请输入营业执照地址').fill('https://cdn.example.com/license-new.png')
    await page.getByPlaceholder('请输入店铺 Logo 地址').fill('https://cdn.example.com/new-logo.png')
    await page.locator('[data-testid=\"shop-save-button\"]').click()

    expect(updatePayload).toEqual({
      name: 'New Store',
      logo: 'https://cdn.example.com/new-logo.png',
      contactName: 'New Owner',
      contactPhone: '13911112222',
      businessLicense: 'https://cdn.example.com/license-new.png'
    })
    await expect(page.locator('text=New Store')).toBeVisible()
    await expect(page.locator('[data-testid=\"shop-edit-button\"]')).toBeVisible()
  })

  test('merchant shop should restore merchantId from token claims when local storage is missing it', async ({ page }) => {
    await page.addInitScript((token) => {
      localStorage.setItem('token', token)
      localStorage.setItem('username', 'merchant-a')
      localStorage.setItem('type', 'merchant')
      localStorage.removeItem('merchantId')
    }, makeJwt({
      sub: '9001',
      username: 'merchant-a',
      role: 'admin',
      type: 'merchant',
      merchantId: 2001
    }))

    await page.route('**/api/v1/admin/merchants/2001', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 2001,
            name: 'Recovered Store',
            logo: '',
            contactName: 'Recovered Owner',
            contactPhone: '13800000000',
            businessLicense: 'https://cdn.example.com/license.png',
            status: 1,
            statusText: '宸查€氳繃',
            createdAt: '2026-05-17 09:00:00',
            reason: ''
          }
        })
      })
    })

    await page.goto('/merchant/shop')

    await expect(page.locator('text=Recovered Store')).toBeVisible()
    await expect.poll(() => page.evaluate(() => localStorage.getItem('merchantId'))).toBe('2001')
  })

  test('merchant shop should replace stale merchantId in local storage with token claims', async ({ page }) => {
    const requestedMerchantIds = []

    await page.addInitScript((token) => {
      localStorage.setItem('token', token)
      localStorage.setItem('username', 'merchant-a')
      localStorage.setItem('type', 'merchant')
      localStorage.setItem('merchantId', '2001')
    }, makeJwt({
      sub: '9001',
      username: 'merchant-a',
      role: 'admin',
      type: 'merchant',
      merchantId: 3001
    }))

    await page.route('**/api/v1/admin/merchants/*', async (route, request) => {
      const merchantId = request.url().split('/').pop()
      requestedMerchantIds.push(merchantId)

      if (merchantId === '3001') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              id: 3001,
              name: 'Token Store',
              logo: '',
              contactName: 'Token Owner',
              contactPhone: '13800000000',
              businessLicense: 'https://cdn.example.com/license.png',
              status: 1,
              statusText: '审核通过',
              createdAt: '2026-05-17 09:00:00',
              reason: ''
            }
          })
        })
        return
      }

      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ code: 404, message: 'merchant not found' })
      })
    })

    await page.goto('/merchant/shop')

    await expect(page.locator('text=Token Store')).toBeVisible()
    await expect.poll(() => page.evaluate(() => localStorage.getItem('merchantId'))).toBe('3001')
    expect(requestedMerchantIds).toEqual(['3001'])
  })

  test('merchant shop should preserve large merchantId from token claims', async ({ page }) => {
    const merchantId = '2053411485924855808'
    const requestedMerchantIds = []

    await page.addInitScript((token) => {
      localStorage.setItem('token', token)
      localStorage.setItem('username', 'merchant-a')
      localStorage.setItem('type', 'merchant')
      localStorage.removeItem('merchantId')
    }, makeJwtFromRawPayload(`{"sub":"9001","username":"merchant-a","role":"admin","type":"merchant","merchantId":${merchantId}}`))

    await page.route('**/api/v1/admin/merchants/*', async (route, request) => {
      const requestedMerchantId = request.url().split('/').pop()
      requestedMerchantIds.push(requestedMerchantId)

      if (requestedMerchantId === merchantId) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              id: merchantId,
              name: 'Large Token Store',
              logo: '',
              contactName: 'Token Owner',
              contactPhone: '13800000000',
              businessLicense: 'https://cdn.example.com/license.png',
              status: 1,
              statusText: '审核通过',
              createdAt: '2026-05-17 09:00:00',
              reason: ''
            }
          })
        })
        return
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 60010006, message: '没有操作该商家的权限' })
      })
    })

    await page.goto('/merchant/shop')

    await expect(page.locator('text=Large Token Store')).toBeVisible()
    await expect.poll(() => page.evaluate(() => localStorage.getItem('merchantId'))).toBe(merchantId)
    expect(requestedMerchantIds).toEqual([merchantId])
  })
})
