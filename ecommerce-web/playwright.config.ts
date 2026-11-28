import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  timeout: 15000,
  retries: 0,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:3000',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { channel: process.env.PLAYWRIGHT_BROWSER_CHANNEL } },
  ],
})
