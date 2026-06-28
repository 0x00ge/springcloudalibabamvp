import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'

import { BUSINESS_CODE, HTTP_STATUS } from '@/constants/httpCode'
import {
  clearStoredAuthInfo,
  getAccessToken,
  isAccessTokenExpired,
  setAuthToken,
} from '@/store/authStore'
import type { AuthTokenParams } from '@/types/authTypes'
import { notify } from '@/utils/notify'

interface ResponseData<T = unknown> {
  code: number
  message: string
  data: T
}

const baseURL = import.meta.env.VITE_API_URL || '/api'

const axiosInstance: AxiosInstance = axios.create({
  baseURL,
  timeout: 5000,
  withCredentials: true,
})

let refreshPromise: Promise<string> | null = null
let isHandleTokenExpired = false

const NO_AUTO_REFRESH_AUTH_API_PATHS = [
  '/auth/login',
  '/auth/refresh',
  '/auth/register',
  '/auth/register/code',
]

const getRequestPath = (url = '') => {
  try {
    return new URL(url, window.location.origin).pathname
  } catch {
    return url.split('?')[0]
  }
}

const isNoAutoRefreshAuthApi = (url = '') => {
  const requestPath = getRequestPath(url)

  return NO_AUTO_REFRESH_AUTH_API_PATHS.some(
    (path) => requestPath === path || requestPath === `/api${path}`,
  )
}

const isRefreshApi = (url = '') => {
  const requestPath = getRequestPath(url)

  return requestPath === '/auth/refresh' || requestPath === '/api/auth/refresh'
}

const handleRefreshAccessToken = async () => {
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
        refreshPromise = null
      })
  }

  return refreshPromise
}

const getOrRefreshAccessToken = async (url?: string) => {
  if (isNoAutoRefreshAuthApi(url)) return undefined

  const accessToken = getAccessToken()

  if (!accessToken || isAccessTokenExpired()) {
    return handleRefreshAccessToken()
  }

  return accessToken
}

const handleTokenExpired = () => {
  clearStoredAuthInfo()

  if (isHandleTokenExpired) return

  isHandleTokenExpired = true
  notify('请重新登录后继续操作', 'warning')

  if (window.location.pathname !== '/login') {
    const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
    window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
  }
}

axiosInstance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    try {
      const token = await getOrRefreshAccessToken(config.url)

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch (error) {
      handleTokenExpired()
      return Promise.reject(error)
    }

    return config
  },
  (error: AxiosError) => {
    notify(error.message, 'error')
    return Promise.reject(error)
  },
)

axiosInstance.interceptors.response.use(
  (response: AxiosResponse<ResponseData>) => {
    if (response.data.code === BUSINESS_CODE.UNAUTHORIZED) {
      if (isRefreshApi(response.config.url)) {
        return Promise.reject(response.data)
      }

      if (isNoAutoRefreshAuthApi(response.config.url)) {
        notify(response.data.message, 'error')
      } else {
        handleTokenExpired()
      }

      return Promise.reject(response.data)
    }

    if (response.data.code !== BUSINESS_CODE.SUCCESS) {
      if (isRefreshApi(response.config.url)) {
        return Promise.reject(response.data)
      }

      notify(response.data.message, 'error')
      return Promise.reject(response.data)
    }

    isHandleTokenExpired = false

    return response
  },
  (error: AxiosError) => {
    if (error.response?.status === HTTP_STATUS.UNAUTHORIZED) {
      if (isRefreshApi(error.config?.url)) {
        return Promise.reject(error)
      }

      if (isNoAutoRefreshAuthApi(error.config?.url)) {
        notify(error.message, 'error')
      } else {
        handleTokenExpired()
      }

      return Promise.reject(error)
    }

    notify(error.message, 'error')
    return Promise.reject(error)
  },
)

export default axiosInstance
