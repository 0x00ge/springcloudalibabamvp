import {computed, reactive, ref} from 'vue'
import {defineStore} from 'pinia'

import {getCurrentAuth, login, logout, refreshAccessToken, register} from '@/api/apiAuth.ts'
import type {AuthTokenParams, CurrentAuthParams, LoginParams} from '@/types/authTypes.ts'
import type {UserInfo} from '@/types/userTypes.ts'

/**
 * accessToken 提前失效窗口。
 *
 * 后端当前 accessToken 有效期是 60 秒，前端缓冲窗口不能大于有效期；
 * 否则登录刚拿到 token 就会被判定为“即将过期”，进入首页时立刻触发 refresh。
 */
const TOKEN_EXPIRE_BUFFER_SECONDS = 5

/**
 * accessToken 内存失效定时器。
 *
 * authToken 中只保留后端返回的 accessTokenExpiresIn，不额外维护绝对过期时间字段。
 * 为了让前端能在 token 快过期时主动刷新，这里用定时器在“剩余秒数进入缓冲窗口”时清掉 accessToken。
 */
let accessTokenExpireTimer: number | undefined

/**
 * 当前浏览器页签内的鉴权运行时状态。
 *
 * 设计约定：
 * 1. accessToken 只放在内存中，页面刷新后会丢失，避免长期暴露在 localStorage/sessionStorage。
 * 2. refreshToken 不进入 JS，后端通过 HttpOnly Cookie 写入浏览器，刷新时浏览器自动携带。
 * 3. accessTokenExpiresIn 直接使用后端返回的剩余秒数，不再派生额外的绝对过期时间字段。
 */
const authToken = reactive<AuthTokenParams>({
    tokenType: '',
    accessToken: '',
    accessTokenExpiresIn: 0,
    refreshToken: undefined,
    refreshTokenExpiresIn: 0,
})

/**
 * 停止旧的 accessToken 失效定时器。
 * 每次登录、刷新、登出都会先清理旧定时器，避免旧 token 的定时任务影响新 token。
 */
const clearAccessTokenExpireTimer = () => {
    if (accessTokenExpireTimer === undefined) return

    window.clearTimeout(accessTokenExpireTimer)
    accessTokenExpireTimer = undefined
}

/**
 * 安排 accessToken 自动进入失效状态。
 *
 * 后端返回的是 accessTokenExpiresIn 秒数。前端不额外维护绝对过期时间字段，
 * 而是在保存 token 时启动一个定时器：当剩余时间进入缓冲窗口后，清空 accessToken，
 * 下一次 axios 请求自然会走 /auth/refresh。
 */
const scheduleAccessTokenExpire = (expiresIn = 0) => {
    clearAccessTokenExpireTimer()

    if (expiresIn <= 0) return

    const expireDelaySeconds = Math.max(expiresIn - TOKEN_EXPIRE_BUFFER_SECONDS, 0)

    accessTokenExpireTimer = window.setTimeout(() => {
        authToken.accessToken = ''
        authToken.accessTokenExpiresIn = 0
        accessTokenExpireTimer = undefined
    }, expireDelaySeconds * 1000)
}

/**
 * accessToken 进入失效状态。
 * 只清理 accessToken 相关字段；refreshToken Cookie 继续由后端管理。
 */
const clearAccessToken = () => {
    clearAccessTokenExpireTimer()

    authToken.tokenType = ''
    authToken.accessToken = ''
    authToken.accessTokenExpiresIn = 0
}

/**
 * 保存登录/刷新接口返回的 token 信息。
 *
 * 这里不保存 refreshToken 原值：
 * - 当前后端的 refreshToken 通过 HttpOnly Cookie 管理；
 * - 即使 DTO 里保留 refreshToken 字段，前端也不依赖它；
 * - 后续刷新只需要调用 /auth/refresh，让浏览器自动带 Cookie。
 */
export const setAuthToken = (tokens: AuthTokenParams) => {
    authToken.tokenType = tokens.tokenType || 'Bearer'
    authToken.accessToken = tokens.accessToken || ''
    authToken.accessTokenExpiresIn = tokens.accessTokenExpiresIn || 0
    authToken.refreshToken = undefined
    authToken.refreshTokenExpiresIn = tokens.refreshTokenExpiresIn || 0

    scheduleAccessTokenExpire(authToken.accessTokenExpiresIn)
}

/**
 * 清理前端保存的登录态。
 *
 * 这个方法只负责 JS 内存状态：
 * - accessToken 从内存清掉；
 * - 当前用户信息由 Pinia action 清掉；
 * - refreshToken Cookie 需要后端 /auth/logout 清理，或等待 Cookie 自然过期。
 */
export const clearStoredAuthInfo = () => {
    clearAccessToken()
    authToken.refreshToken = undefined
    authToken.refreshTokenExpiresIn = 0
}

/**
 * 给 axios、路由守卫读取当前 accessToken。
 * 空字符串统一转换成 undefined，调用方只需要判断 truthy/falsy。
 */
export const getAccessToken =
    () => authToken.accessToken || undefined

/**
 * 判断 accessToken 是否已经过期或即将过期。
 * 没有 token、没有过期时间、进入提前刷新窗口，都视为不可继续直接使用。
 */
export const isAccessTokenExpired =
    () => {
        if (!authToken.accessToken || authToken.accessTokenExpiresIn <= 0) return true

        return authToken.accessTokenExpiresIn <= TOKEN_EXPIRE_BUFFER_SECONDS
    }

export const useAuthStore =
    defineStore('auth', () => {
        /**
         * 当前登录用户基础信息。
         *
         * loginAction 只负责拿 token，不额外请求用户详情；
         * AppLayout 挂载后会调用 loadCurrentAuthAction，由 /auth/me 返回真实用户信息。
         */
        const currentAuth = ref<CurrentAuthParams>()

        /**
         * 是否处于已登录运行态。
         * 这里只判断当前内存中是否有 accessToken，不代表 refreshToken Cookie 一定有效。
         */
        const isLogin = computed(() => Boolean(getAccessToken()))

        /**
         * 是否可以直接发起需要登录的业务请求。
         * 和 isLogin 不同，这里还会把“即将过期”的 accessToken 判为无效。
         */
        const hasValidLogin = () => Boolean(getAccessToken() && !isAccessTokenExpired())

        /**
         * 顶部栏需要的用户展示模型。
         * 后端 CurrentAuthDTO 只返回认证维度字段，这里转换成布局组件通用的 UserInfo。
         */
        const currentUserInfo = computed<UserInfo>(() => {
            if (!currentAuth.value) {
                return {id: '', name: '', phone: '', email: '', role: '', status: ''}
            }

            const displayName = currentAuth.value.name || currentAuth.value.phone || '用户'

            return {
                id: currentAuth.value.id || '',
                name: displayName,
                phone: currentAuth.value.phone,
                email: '',
                role: '',
                status: '',
            }
        })

        /**
         * 清理 Pinia 用户信息和 token 内存缓存。
         * 这个 action 不调用后端，适合路由守卫、axios 401 处理这类“本地兜底清理”场景。
         */
        const clearLoginState = () => {
            currentAuth.value = undefined
            clearStoredAuthInfo()
        }

        /**
         * 登录流程。
         *
         * 1. 调用 /auth/login 校验手机号和密码。
         * 2. 后端响应体返回 accessToken，并通过 HttpOnly Cookie 写入 refreshToken。
         * 3. 前端只保存 accessToken 及其过期时间，后续业务请求由 axios 自动写入 Authorization。
         */
        const loginAction =
            async (params: LoginParams) => {
                const tokenResult = await login(params)

                setAuthToken(tokenResult)

                return tokenResult
            }

        /**
         * 注册流程。
         *
         * 注册成功只返回用户基础信息，不自动写入登录态；
         * 页面会切回登录表单，让用户显式登录并获取 token。
         */
        const registerAction =
            async (params: CurrentAuthParams) => register(params)

        /**
         * 加载当前登录用户。
         *
         * 前端不会伪造 X-User-Id；该请求先经过 gateway 校验 accessToken，
         * 再由 gateway 把当前用户 ID 透传给 user 服务。
         */
        const getAuthAction =
            async () => {
                currentAuth.value = await getCurrentAuth()

                return currentAuth.value
            }

        /**
         * 恢复登录态。
         *
         * 1. 当前页签内 accessToken 仍可用时，直接复用。
         * 2. accessToken 缺失或即将过期时，调用 /auth/refresh。
         * 3. refreshToken 由浏览器自动携带 Cookie，前端不读取 Cookie 内容。
         * 4. 刷新失败说明登录态不可恢复，清理本地状态后继续向外抛错。
         */
        const refreshLoginStateAction =
            async () => {
                const accessToken = getAccessToken()

                if (accessToken && !isAccessTokenExpired()) {
                    return accessToken
                }

                try {
                    const tokenResult = await refreshAccessToken()

                    setAuthToken(tokenResult)

                    return tokenResult.accessToken
                } catch (error) {
                    clearLoginState()

                    throw error
                }
            }

        /**
         * 退出登录。
         *
         * 有 accessToken 时先通知后端拉黑当前 token、删除 refreshToken 白名单并清 Cookie；
         * 无论接口成功失败，finally 都会清理前端内存，避免页面继续显示已登录状态。
         */
        const logoutAction =
            async () => {
                try {
                    if (getAccessToken()) {
                        await logout()
                    }
                } finally {
                    clearLoginState()
                }
            }

        return {
            authToken,
            currentAuth,
            currentUserInfo,
            isLogin,
            hasValidLogin,
            clearLoginState,
            loginAction,
            registerAction,
            getAuthAction,
            refreshLoginStateAction,
            logoutAction,
        }
    })
