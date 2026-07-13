import { computed, reactive } from 'vue'
import { defineStore } from 'pinia'
import type { AuthTokenParams } from '@/types/authTypes.ts'

/**
 * Token 提前失效缓冲窗口（秒）。
 *
 * 后端 AccessToken 有效期为 60 秒，前端在剩余 5 秒时即视为"已过期"，
 * 主动触发刷新，避免请求到达后端时 Token 刚好失效返回 401。
 *
 * @注意 该值必须小于后端 AccessToken 有效期，否则登录后立即触发刷新。
 */
const TOKEN_EXPIRE_BUFFER_SECONDS = 5

/** Token 失效定时器句柄，用于在 Token 即将过期时主动清空内存。 */
let accessTokenExpireTimer: number | undefined

/**
 * 认证令牌状态。
 *
 * 设计约定：
 * - AccessToken 仅存于内存，页面刷新即丢失，防御 XSS 窃取。
 * - RefreshToken 由后端通过 HttpOnly Cookie 下发，JS 不可读，浏览器自动携带。
 * - `accessTokenExpiresIn` 直接使用后端返回的相对秒数，不派生绝对时间戳。
 */
const authToken = reactive<AuthTokenParams>({
    type: '',
    accessToken: '',
    accessTokenExpiresIn: 0,
})

/** 清除失效定时器，避免旧 Token 的定时任务影响新 Token。 */
const stopAccessTokenExpireTimer = () => {
    if (accessTokenExpireTimer === undefined) return
    window.clearTimeout(accessTokenExpireTimer)
    accessTokenExpireTimer = undefined
}

/**
 * 启动 Token 失效定时器。
 *
 * 在 `expiresIn - 缓冲窗口` 秒后清空内存中的 AccessToken，
 * 使下一次业务请求因无 Token 而触发 `/auth/refresh`。
 */
const setAccessTokenExpire = (expiresIn: number) => {
    const delaySeconds = Math.max(expiresIn - TOKEN_EXPIRE_BUFFER_SECONDS, 0)

    accessTokenExpireTimer = window.setTimeout(() => {
        authToken.accessToken = ''
        authToken.accessTokenExpiresIn = 0
        accessTokenExpireTimer = undefined
    }, delaySeconds * 1000)
}

/**
 * 保存认证令牌。
 *
 * @param token 后端返回的令牌数据，包含类型、AccessToken 及其有效期。
 *
 * @example
 * setAuthToken({ type: 'Bearer', accessToken: 'xxx', accessTokenExpiresIn: 60 })
 */
export const setAuthToken = (token: AuthTokenParams) => {
    stopAccessTokenExpireTimer()

    authToken.type = token.type || 'Bearer'
    authToken.accessToken = token.accessToken || ''
    authToken.accessTokenExpiresIn = token.accessTokenExpiresIn || 0

    if (authToken.accessTokenExpiresIn > 0) {
        setAccessTokenExpire(authToken.accessTokenExpiresIn)
    }
}

/**
 * 清理内存中的认证令牌。
 *
 * 仅清理前端状态，不涉及后端操作。
 * RefreshToken Cookie 由后端 `/auth/logout` 接口清理或等待自然过期。
 */
export const clearAuthToken = () => {
    stopAccessTokenExpireTimer()
    authToken.type = ''
    authToken.accessToken = ''
    authToken.accessTokenExpiresIn = 0
}

/**
 * 获取当前 AccessToken 字符串。
 *
 * 供 Axios 请求拦截器使用，自动注入 `Authorization` 头。
 *
 * @returns Token 字符串，无 Token 时返回空字符串。
 */
export const getAccessToken = () => authToken.accessToken

/**
 * 判断 AccessToken 是否已过期或即将过期。
 *
 * 供路由守卫和 Axios 拦截器使用，决定是否需要提前刷新。
 *
 * @returns `true` 表示 Token 不可用，需刷新；`false` 表示可直接使用。
 */
export const isAccessTokenExpired = () => {
    if (!authToken.accessToken || authToken.accessTokenExpiresIn <= 0) return true
    return authToken.accessTokenExpiresIn <= TOKEN_EXPIRE_BUFFER_SECONDS
}

// ============================================================
// Pinia Store
// ============================================================

export const useAuthStore = defineStore('auth', () => {
    /**
     * 用户是否处于登录状态。
     *
     * 仅依据内存中是否存在 AccessToken 判断，不保证 RefreshToken 仍有效。
     * 页面刷新后该值为 `false`，需由路由守卫通过 RefreshToken 静默恢复。
     */
    const isLogin = computed(() => Boolean(getAccessToken()))

    return {
        // ---------- 状态 ----------
        /** 认证令牌原始数据，仅用于调试，生产环境不建议直接修改。 */
        authToken,

        // ---------- 计算属性 ----------
        isLogin,

        // ---------- 操作 ----------
        setAuthToken,
        clearAuthToken,
        getAccessToken,
        isAccessTokenExpired,
    }
})