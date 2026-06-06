import { createRouter, createWebHistory } from 'vue-router'

import { guard } from '@/router/guard.ts'
import { routes } from '@/router/routes.ts'

const router = createRouter({
  // 使用 HTML5 history 模式，BASE_URL 由 Vite 根据部署路径注入。
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

guard(router)

export default router
