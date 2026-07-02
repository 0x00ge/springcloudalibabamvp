import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { notification } from 'antd'

import { BUSINESS_CODE, HTTP_STATUS } from '@/constants/httpCode'
import type { AuthTokenParams } from '@/types/authTypes'
import {
  clearStoredAuthInfo,
  getAccessToken,
  isAccessTokenExpired,
  setAuthToken,
} from '@/stores/authStore'

/**
 * axios 请求总流程：
 * 1. 页面调用 api 文件中的方法，例如 fetchUsers。
 * 2. api 方法调用 utils/http/http.ts 中的 get/post。
 * 3. get/post 使用这里创建的 axiosInstance 发请求。
 * 4. 请求拦截器先检查内存中的 accessToken 是否可用。
 * 5. accessToken 可用：直接放到 Authorization 请求头。
 * 6. accessToken 缺失或过期：调用 /auth/refresh，浏览器自动携带 HttpOnly refreshToken Cookie。
 * 7. refreshToken Cookie 无效：清理内存状态，并跳回登录页。
 * 8. 业务接口返回 401：直接认为登录态失效，清理内存状态，并跳回登录页。
 * 9. 其他错误：统一使用 Ant Design 消息提示。
 */

interface ResponseData<T = unknown> {
  code: number
  message: string
  data: T
}

// baseURL 优先读取环境变量；没有配置时使用 /api，由 Vite 代理转发到 gateway。
const baseURL = import.meta.env.VITE_API_URL || '/api'

// 创建 axios 实例。
// 后续项目中不要直接使用 axios.get/axios.post，而是统一使用这个实例，保证拦截器一定生效。
const axiosInstance: AxiosInstance = axios.create({
  baseURL,
  timeout: 5000,
  withCredentials: true,
})

// 刷新 token 的共享 Promise。
// 当多个请求同时发现 accessToken 过期时，只发起一次 /auth/refresh，其他请求复用这个结果。
let refreshPromise: Promise<string> | null = null

// 登录失效处理标记。多个接口同时返回 401 时，只需要弹一次提示、跳一次登录页，避免页面连续闪动。
let isHandleTokenExpired = false

const NO_AUTO_REFRESH_AUTH_API_PATHS = ['/auth/login', '/auth/refresh', '/auth/register', '/auth/register/code']

// 统一把请求地址转成 pathname，方便兼容下面几种写法：
// - /auth/login
// - /auth/login?type=account
// - http://localhost:5173/auth/login
// - http://localhost:5173/api/auth/login
const getRequestPath = (url = '') => {
  try {
    return new URL(url, window.location.origin).pathname
  } catch {
    return url.split('?')[0]
  }
}

// 流程节点：登录、刷新、登出接口本身不参与自动刷新，避免刷新请求互相套娃。
// /auth/login：登录时还没有 token，不需要刷新。
// /auth/refresh：它自己就是刷新 token 的接口，不能再触发刷新自己。
// /auth/logout 需要携带 Authorization，让后端能拉黑当前 accessToken。
const isNoAutoRefreshAuthApi = (url = '') => {
  const requestPath = getRequestPath(url)

  return NO_AUTO_REFRESH_AUTH_API_PATHS.some((path) => requestPath === path || requestPath === `/api${path}`)
}

const isRefreshApi = (url = '') => {
  const requestPath = getRequestPath(url)

  return requestPath === '/auth/refresh' || requestPath === '/api/auth/refresh'
}

/**
 * 获取 accessToken。
 *
 * 1. 如果是 /auth/login、/auth/refresh，不自动刷新。
 * 2. accessToken 缺失或过期时，调用 /auth/refresh 尝试恢复登录态。
 * 3. 如果 accessToken 正常，直接返回。
 */
const getOrRefreshAccessToken = async (url?: string) => {
  // 登录、刷新接口不走自动刷新逻辑，也不携带旧的 Authorization。
  // 例如：用户重新登录时，如果请求头还带着旧 accessToken，部分后端可能会误判。
  if (isNoAutoRefreshAuthApi(url)) return undefined

  const accessToken = getAccessToken()

  // accessToken 缺失或过期时，尝试用 HttpOnly Cookie 中的 refreshToken 恢复。
  if (!accessToken || isAccessTokenExpired()) {
    return handleRefreshAccessToken()
  }

  return accessToken
}

/**
 * 刷新 accessToken。
 *
 * 1. 如果当前已经有刷新请求在进行中，直接复用 refreshPromise。
 * 2. 刷新请求不传 refreshToken 参数，浏览器会自动携带 HttpOnly Cookie。
 * 3. 刷新成功后保存新的 accessToken；新的 refreshToken 继续由后端写 Cookie。
 *
 * 注意：
 * 这里故意使用 axios.post，而不是 axiosInstance.post。
 * 因为 axiosInstance 带有请求拦截器，如果刷新接口也走 axiosInstance，可能又触发 getValidAccessToken，
 * 从而造成“刷新 token 的请求又去刷新 token”的循环。
 */
const handleRefreshAccessToken = async () => {
  // refreshPromise 为空，说明当前没有刷新请求在进行中，需要新发起一次。
  if (!refreshPromise) {
    refreshPromise = axios
      .post<ResponseData<AuthTokenParams>>('/auth/refresh', undefined, {
        baseURL,
        timeout: 5000,
        withCredentials: true,
      })
      .then((response) => {
        if (response.data.code !== BUSINESS_CODE.SUCCESS) {
          throw new Error(response.data.message)
        }
        const data = response.data.data
        setAuthToken(data)
        return data.accessToken
      })
      .finally(() => {
        // 无论刷新成功还是失败，都要清空 refreshPromise。
        // 否则下一次 accessToken 过期时，会一直复用旧 Promise。
        refreshPromise = null
      })
  }

  return refreshPromise
}

/**
 * 处理 tokenExpired。
 *
 * 1. 统一清理 token。
 * 2. 并跳回登录页。
 */
const handleTokenExpired = () => {
  // 清理内存中的 accessToken；refreshToken Cookie 由后端清理或自然过期。
  clearStoredAuthInfo()

  // 如果已经在处理登录失效，就不重复弹通知、不重复修改 location。
  if (isHandleTokenExpired) return

  isHandleTokenExpired = true

  notification.warning({
    message: '登录已失效',
    description: '请重新登录后继续操作',
  })

  // 如果当前已经在登录页，就不重复跳转。
  if (window.location.pathname !== '/login') {
    // redirect 保存当前访问地址。
    // 用户重新登录后，Login 页面会读取 redirect，把用户带回原来想访问的页面。
    const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`

    window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
  }
}

/**
 * 请求拦截器：
 *
 */
axiosInstance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    try {
      const token = await getOrRefreshAccessToken(config.url)
      // accessToken 放到 Authorization 请求头，Bearer 是一种常见 token 认证格式。
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch (error) {
      // 请求发送前刷新失败，直接清理并跳登录。
      handleTokenExpired()
      return Promise.reject(error)
    }

    return config
  },
  (error: AxiosError) => {
    // 请求还没发出去就出错，例如配置错误、拦截器内部异常。
    notification.error({
      message: '错误',
      description: error.message,
    })

    return Promise.reject(error)
  },
)

/**
 * 响应拦截器：
 *
 */
axiosInstance.interceptors.response.use(
  (response: AxiosResponse<ResponseData>) => {
    // 后端业务 code 为 401：直接认为登录态失效。
    // 注意：这里不再尝试刷新 token，也不重放原请求。
    // accessToken 过期的正常刷新已经在“请求拦截器”里提前处理了；
    // 如果接口仍然返回 401，说明后端已经明确拒绝当前登录态，继续请求没有意义。
    if (response.data.code === BUSINESS_CODE.UNAUTHORIZED) {
      // refresh 失败通常只是表示 Cookie 不存在或已失效，交给调用方决定是否跳登录页。
      if (isRefreshApi(response.config.url)) {
        return Promise.reject(response.data)
      }

      // 登录这类认证接口自己的 401，只展示后端错误，不当成“业务登录态失效”重复跳转。
      if (isNoAutoRefreshAuthApi(response.config.url)) {
        notification.error({
          message: '错误',
          description: response.data.message,
        })
      } else {
        handleTokenExpired()
      }

      return Promise.reject(response.data)
    }

    // 非 200 业务 code：统一弹出后端返回的错误信息。
    if (response.data.code !== BUSINESS_CODE.SUCCESS) {
      // refresh 失败通常只是表示 Cookie 不存在、过期或已被使用过。
      // 这类错误交给路由守卫判断是否跳登录页，不在这里弹错误提示。
      if (isRefreshApi(response.config.url)) {
        return Promise.reject(response.data)
      }

      notification.error({
        message: '错误',
        description: response.data.message,
      })

      return Promise.reject(response.data)
    }

    isHandleTokenExpired = false

    // 业务成功时，把完整 response 交给 http.ts。
    // http.ts 会继续取 response.data.data 返回给页面。
    return response
  },
  (error: AxiosError) => {
    // HTTP 状态码为 401：直接认为登录态失效。
    // 这里也不再尝试刷新 token，避免后端已经拒绝后前端继续重复请求。
    if (error.response?.status === HTTP_STATUS.UNAUTHORIZED) {
      // refresh 失败通常只是表示 Cookie 不存在或已失效，交给调用方决定是否跳登录页。
      if (isRefreshApi(error.config?.url)) {
        return Promise.reject(error)
      }

      // 登录这类认证接口自己的 HTTP 401，只展示错误，不重复走登录失效跳转。
      if (isNoAutoRefreshAuthApi(error.config?.url)) {
        notification.error({
          message: '错误',
          description: error.message,
        })
      } else {
        handleTokenExpired()
      }

      return Promise.reject(error)
    }

    // 其他网络错误、超时错误，统一给用户提示。
    notification.error({
      message: '错误',
      description: error.message,
    })

    return Promise.reject(error)
  },
)

export default axiosInstance
