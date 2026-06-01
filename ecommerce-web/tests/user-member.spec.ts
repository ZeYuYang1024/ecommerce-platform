import { test, expect } from '@playwright/test'

test.describe('PC User Member', () => {

  test.beforeEach(async ({ page }) => {
    await page.context().addCookies([
      { name: 'token', value: 'mock-token', domain: 'localhost', path: '/' },
      { name: 'username', value: 'member-user', domain: 'localhost', path: '/' }
    ])
  })

  test('member center uses a single api prefix for member requests', async ({ page }) => {
    const requestedUrls: string[] = []

    await page.route('**/api/v1/member/profile', async (route) => {
      requestedUrls.push(route.request().url())
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            level: { name: '普通会员', levelCode: 'REGULAR', sortOrder: 1, pointsMultiplier: '1.00' },
            growthValue: 20,
            totalGrowthValue: 20,
            nextLevelGrowth: 1000,
            availablePoints: 5
          }
        })
      })
    })

    await page.route('**/api/v1/member/check-in/status', async (route) => {
      requestedUrls.push(route.request().url())
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            checkedToday: false,
            consecutiveDays: 0,
            pointsAwardedToday: 0
          }
        })
      })
    })

    await page.goto('/user')

    await expect(page.locator('h1')).toContainText('个人中心')
    await expect(page.getByText('普通会员')).toBeVisible()

    expect(requestedUrls.some((url) => url.includes('/api/v1/api/v1/member/'))).toBeFalsy()
    expect(requestedUrls.some((url) => url.endsWith('/api/v1/member/profile'))).toBeTruthy()
    expect(requestedUrls.some((url) => url.endsWith('/api/v1/member/check-in/status'))).toBeTruthy()
  })
})
