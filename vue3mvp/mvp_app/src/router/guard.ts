import type {Router} from 'vue-router'

import {getAccessToken, useAuthStore} from '@/stores/authStore.ts'

/**
 * 注册全局路由守卫，统一处理登录重定向、鉴权拦截和登录态恢复。
 */
export const guard = (router: Router) => {
    // 每次路由跳转时读取认证状态，确保守卫使用最新的登录信息。
    router.beforeEach(
        async (to) => {
            const authStore = useAuthStore()

            // 进入登录页时只检查当前页签内是否已有 accessToken。
            // 未登录用户刷新 /login 时不主动调用 /auth/refresh，避免没有 refreshToken Cookie 时后端报错。
            // 已登录用户刷新受保护页面时，会在下面的 isAuth 分支里恢复登录态。
            if (to.path === '/login') {
                if (getAccessToken()) {
                    return (to.query.redirect as string) || '/home'
                }

                authStore.clearLoginState()
                return
            }

            // 访问受保护页面时，先尝试从内存 accessToken 或 refreshToken 恢复登录态。
            if (to.meta.isAuth) {
                try {
                    await authStore.refreshLoginStateAction()
                    if (!authStore.currentAuth) {
                        await authStore.getAuthAction()
                    }
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
