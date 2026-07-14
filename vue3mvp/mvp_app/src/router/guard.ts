import type { Router, RouteLocationNormalized } from 'vue-router'
import { refreshAccessToken } from '@/api/apiUser.ts'
import { useAuthStore } from '@/stores/authStore'
import { useUserStore } from '@/stores/userStore'

/** 登录白名单：不需要认证即可访问的页面 */
const LOGIN_WHITELIST = ['/login', '/register', '/forgot-password']

/** 登录成功后默认跳转地址 */
const DEFAULT_REDIRECT = '/home'

/**
 * 判断路由是否需要认证。
 * 默认需要认证，白名单页面除外。
 */
const isRequireAuth = (to: RouteLocationNormalized): boolean => {
    // 显式标记为不需要认证
    if (to.meta.requireAuth === false) return false
    // 在白名单中
    if (LOGIN_WHITELIST.includes(to.path)) return false
    // 默认需要认证
    return true
}

/**
 * 注册全局路由守卫。
 *
 * 职责：
 * 1. 登录页重定向：已登录用户访问 /login → 跳转首页
 * 2. 静默恢复：页面刷新后，通过 RefreshToken 恢复登录态
 * 3. 鉴权拦截：未登录用户访问受保护页面 → 跳转登录页
 */
export const guard = (router: Router) => {
    router.beforeEach(async (to, _from, next) => {
        const authStore = useAuthStore()
        const userStore = useUserStore()

        const redirectToLogin = () => {
            authStore.clearAuthToken()
            userStore.clearUserInfo()

            next({
                path: '/login',
                query: {
                    // 登录成功后跳回原页面。
                    redirect: to.fullPath,
                },
            })
        }

        const loadCurrentUser = async () => {
            if (!userStore.currentAuth) {
                await userStore.fetchUserInfo()
            }
        }

        /**
         * 1. 登录页特殊处理
         */
        if (to.path === '/login') {
            // 已登录 → 跳转到首页或 redirect 参数指定的页面
            if (authStore.hasValidToken) {
                const redirect = (to.query.redirect as string) || DEFAULT_REDIRECT
                next(redirect)
                return
            }
            // 内存中有过期 Token 时清掉，避免登录页继续显示旧登录态。
            authStore.clearAuthToken()
            userStore.clearUserInfo()

            // 未登录 → 正常进入登录页
            next()
            return
        }

        /**
         * 2. 不需要认证的页面（公开页面）
         */
        if (!isRequireAuth(to)) {
            next()
            return
        }

        /**
         * 3. 需要认证的页面 → 尝试恢复登录态
         */
        // 3.1 内存已有可用 Token → 加载用户信息后放行
        if (authStore.hasValidToken) {
            try {
                await loadCurrentUser()
            } catch {
                redirectToLogin()
                return
            }

            next()
            return
        }

        // 3.2 内存无 Token → 尝试用 RefreshToken 静默恢复
        try {
            // 调用刷新接口，浏览器自动携带 HttpOnly Cookie
            const tokenResult = await refreshAccessToken()
            authStore.setAuthToken(tokenResult)

            // 恢复用户信息
            await loadCurrentUser()

            // 恢复成功，继续访问目标页面
            next()
            return
        } catch {
            // 3.3 RefreshToken 也过期了 → 跳转登录页
            redirectToLogin()
            return
        }
    })
}
