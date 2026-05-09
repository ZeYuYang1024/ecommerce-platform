export default defineNuxtConfig({
  compatibilityDate: '2025-05-09',
  devtools: { enabled: false },
  modules: ['@pinia/nuxt', '@nuxtjs/tailwindcss'],
  nitro: {
    devProxy: {
      '/api': { target: 'http://localhost:8080/api', changeOrigin: true }
    }
  },
  app: {
    head: {
      title: 'MERCH - 品质电商',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' }
      ],
      link: [
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap' }
      ]
    }
  },
  runtimeConfig: {
    public: {
      apiBase: '/api/v1'
    }
  }
})
