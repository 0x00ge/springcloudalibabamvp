import axios, {
    AxiosError,
    type AxiosInstance,
    type AxiosResponse,
    HttpStatusCode,
    type InternalAxiosRequestConfig,
} from 'axios'
import {ElNotification} from 'element-plus'
import type {AuthTokenParams} from '@/types/authTypes.ts'
import {useAuthStore} from '@/stores/authStore.ts'

interface ResponseResult<T = any> {
    code: number
    message: string
    data: T
}

const baseURL = import.meta.env.VITE_API_URL || '/api'

/** 不需要携带 Token 的认证相关接口 */
const AUTH_WHITELIST = [
    '/auth/refresh',
    '/auth/login',
    '/auth/register',
    '/auth/register/code',
]

const axiosInstance: AxiosInstance = axios.create({
    baseURL,
    timeout: 5000,
    withCredentials: true,
})

/** 刷新 Promise 锁，防止并发刷新 */
let refreshPromise: Promise<string> | null = null

/** 登录失效处理锁，防止重复弹窗跳转 */
let isHandlingTokenExpired = false

/**
 * 标准化 URL，提取路径部分。
 * 兼容完整 URL 和相对路径。
 */
const normalizeUrl = (url = ''): string => {
    try {
        return new URL(url, window.location.origin).pathname
    } catch {
        return url.split('?')[0] || ''
    }
}

/**
 * 判断是否为认证相关接口（登录/注册/刷新等）。
 * 这类接口不参与自动刷新，且错误处理方式特殊。
 */
const isAuthRelatedApi = (url = ''): boolean => {
    const path = normalizeUrl(url)
    return AUTH_WHITELIST.some(
        (whitelist) => path === whitelist || path === `/api${whitelist}`
    )
}

/**
 * 判断是否为刷新接口。
 */
const isRefreshApi = (url = ''): boolean => {
    const path = normalizeUrl(url)
    return path === '/auth/refresh' || path === '/api/auth/refresh'
}

/**
 * 处理登录失效：清理状态 → 弹窗 → 跳转登录页。
 * 防抖处理，多个请求同时触发时只执行一次。
 */
const handleTokenExpired = (): void => {
    const authStore = useAuthStore()

    authStore.clearAuthToken()

    if (isHandlingTokenExpired) return
    isHandlingTokenExpired = true

    ElNotification({
        title: '登录已失效',
        message: '请重新登录后继续操作',
        type: 'error',
        duration: 3000,
    })

    if (window.location.pathname === '/login') return

    const redirect = encodeURIComponent(
        window.location.pathname + window.location.search + window.location.hash
    )
    window.location.href = `/login?redirect=${redirect}`
}

/**
 * 刷新 AccessToken。
 *
 * 核心机制：
 * 1. 如果已有刷新请求在进行，直接复用 Promise。
 * 2. 使用原生 axios（非 axiosInstance）避免循环。
 * 3. 浏览器自动携带 HttpOnly Cookie 中的 RefreshToken。
 * 4. 刷新成功后只把新 Token 写入 authStore。
 * 5. 刷新失败只清理 authStore，跳转动作交给调用方统一处理。
 */
const refreshAccessToken = (): Promise<string> => {
    if (refreshPromise) {
        return refreshPromise
    }

    refreshPromise = axios
        .post<ResponseResult<AuthTokenParams>>(
            '/auth/refresh',
            undefined,
            {baseURL, timeout: 5000, withCredentials: true}
        )
        .then((response) => {
            if (response.data.code !== HttpStatusCode.Ok) {
                throw new Error(response.data.message || '刷新 Token 失败')
            }

            const authStore = useAuthStore()
            const token = response.data.data
            authStore.setAuthToken(token)

            return token.accessToken
        })
        .catch((error) => {
            const authStore = useAuthStore()

            authStore.clearAuthToken()
            throw error
        })
        .finally(() => {
            refreshPromise = null
            // 注意：isHandlingTokenExpired 不在这里重置
            // 由后续成功的业务请求重置
        })

    return refreshPromise
}

/**
 * 获取有效的 AccessToken。
 *
 * 策略：
 * 1. 认证白名单接口 → 不携带 Token
 * 2. 内存 Token 有效 → 直接返回
 * 3. Token 缺失或过期 → 复用 refreshPromise 刷新
 */
const getValidAccessToken = async (
    config: InternalAxiosRequestConfig
): Promise<string | undefined> => {
    const url = config.url

    // 白名单接口不需要 Token
    if (isAuthRelatedApi(url)) {
        return undefined
    }

    const authStore = useAuthStore()
    const token = authStore.getAccessToken()

    // Token 有效 → 直接使用
    if (token && authStore.hasValidToken) {
        return token
    }

    // Token 无效 → 尝试刷新
    try {
        return await refreshAccessToken()
    } catch (error) {
        // 刷新失败，跳转登录
        handleTokenExpired()
        throw error
    }
}

/**
 * 请求拦截器。
 */
axiosInstance.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
        try {
            const token = await getValidAccessToken(config)
            if (token) {
                config.headers.Authorization = `Bearer ${token}`
            }
            return config
        } catch (error) {
            // 错误已在 getValidAccessToken 中处理，这里直接拒绝
            return Promise.reject(error)
        }
    }, (error: AxiosError) => {
        ElNotification({
            title: '请求配置错误',
            message: error.message,
            type: 'error',
            duration: 3000,
        })
        return Promise.reject(error)
    }
)

/**
 * 响应拦截器。
 */
axiosInstance.interceptors.response.use(
    (response: AxiosResponse<ResponseResult>) => {
        const {data, config} = response

        // ----- 业务 401（未授权）-----
        if (data.code === HttpStatusCode.Unauthorized) {
            // 认证接口自己的 401 → 只展示错误，不跳转
            if (isAuthRelatedApi(config.url)) {
                ElNotification({
                    title: '认证失败',
                    message: data.message || '请检查账号信息',
                    type: 'error',
                    duration: 3000,
                })
                return Promise.reject(data)
            }

            // 业务接口 401 → 登录态失效
            handleTokenExpired()
            return Promise.reject(data)
        }

        // ----- 业务非 200（业务错误）-----
        if (data.code !== HttpStatusCode.Ok) {
            // 刷新接口的业务错误 → 静默失败，由调用方处理
            if (isRefreshApi(config.url)) {
                return Promise.reject(data)
            }

            ElNotification({
                title: '操作失败',
                message: data.message || '未知错误',
                type: 'error',
                duration: 3000,
            })
            return Promise.reject(data)
        }

        // ----- 业务成功 -----
        // 重置登录失效锁（说明 Token 已恢复正常）
        isHandlingTokenExpired = false
        return response
    }, (error: AxiosError<ResponseResult>) => {
        const {response, config} = error

        // ----- HTTP 401（网络层未授权）-----
        if (response?.status === HttpStatusCode.Unauthorized) {
            // 认证接口自身的 401 → 只展示错误
            if (isAuthRelatedApi(config?.url)) {
                ElNotification({
                    title: '认证失败',
                    message: error.message,
                    type: 'error',
                    duration: 3000,
                })
                return Promise.reject(error)
            }

            // 业务接口 401 → 登录态失效
            handleTokenExpired()
            return Promise.reject(error)
        }

        // ----- 网络错误 / 超时 -----
        ElNotification({
            title: '网络请求失败',
            message: error.message || '请检查网络连接',
            type: 'error',
            duration: 3000,
        })
        return Promise.reject(error)
    }
)

export default axiosInstance
