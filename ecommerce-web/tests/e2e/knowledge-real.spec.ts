import { test, expect } from '@playwright/test'

async function loginByApi(request: any) {
  const response = await request.post('http://localhost:8080/api/v1/auth/login', {
    data: {
      username: 'zhangsan',
      password: '123456',
    },
  })

  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  expect(body.code).toBe(200)
  return body.data.token as string
}

test.describe('Knowledge Page [REAL API]', () => {
  test.describe.configure({ timeout: 60000 })

  test('guest asking personal question is prompted to log in', async ({ page }) => {
    await page.goto('/knowledge')

    const input = page.locator('input[placeholder=\"输入您的问题...\"]')
    const sendButton = page.locator('button:has-text(\"发送\")')
    await expect(input).toBeVisible()
    await input.fill('我购物车里有没有东西')
    await expect(sendButton).toBeEnabled()
    await sendButton.click()

    const assistantMessages = page.locator('.ai-msg')
    await expect(assistantMessages.last()).toContainText('请先登录后再使用智能客服。', { timeout: 20000 })
  })

  test('logged-in user can query all six knowledge scenarios', async ({ page, context, request }) => {
    const token = await loginByApi(request)
    await context.addCookies([
      {
        name: 'token',
        value: token,
        domain: 'localhost',
        path: '/',
      },
    ])

    await page.goto('/knowledge')

    const input = page.locator('input[placeholder=\"输入您的问题...\"]')
    const sendButton = page.locator('button:has-text(\"发送\")')
    await expect(input).toBeVisible()
    const assistantMessages = page.locator('.ai-msg')
    // 六类实时知识问答都应该可查询 / all six realtime knowledge scenarios should be queryable
    const cases = [
      { question: '我购物车里有没有东西', expected: '购物车' },
      { question: '我有订单吗', expected: '订单' },
      { question: '我有可用优惠券吗', expected: '优惠券' },
      { question: '我的默认收货地址是什么', expected: '地址' },
      { question: '我最近有什么通知', expected: '通知' },
      { question: '订单号202605101214420513的支付状态是什么', expected: '支付状态' },
    ]

    for (const item of cases) {
      await input.fill(item.question)
      await expect(sendButton).toBeEnabled()
      await sendButton.click()
      await expect(assistantMessages.last()).toContainText(item.expected, { timeout: 20000 })
      // 用户端不展示知识库参考来源 / user-facing page should not display knowledge reference sources
      await expect(page.getByText('参考来源')).toHaveCount(0)
    }
  })
})
