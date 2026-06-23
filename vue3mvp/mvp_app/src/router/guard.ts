import type { Router } from 'vue-router'

import { getAccessToken, useAuthStore } from '@/stores/authStore.ts'

/**
 * 注册全局路由守卫，统一处理登录重定向、鉴权拦截和登录态恢复。
 */
export const guard = (router: Router) => {
  // 每次路由跳转时读取认证状态，确保守卫使用最新的登录信息。
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()

    // 进入登录页时不主动调用 /auth/refresh，避免后端未启动时打开登录页就出现 502。
    // 如果当前前端内存里已经有 accessToken，才直接回后台首页。
    if (to.path === '/login') {
      if (getAccessToken()) {
        return '/home'
      }

      authStore.clearLoginState()
    }

    // 访问受保护页面时，先尝试从内存 accessToken 或 refreshToken 恢复登录态。
    if (to.meta.requiresAuth) {
      try {
        await authStore.refreshLoginStateAction()
      } catch {
        authStore.clearLoginState()

        return {
          path: '/login',
          query: {
            // 登录成功后 LoginView 会读取 redirect，并跳回用户原本想访问的页面。
            redirect: to.fullPath,
          },
        }
      }
    }
  })
}
