import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

import {
  getCurrentAuth,
  login,
  logout,
  refreshAccessToken,
  register,
} from '@/api/apiAuth'
import {
  clearStoredAuthInfo,
  getAccessToken,
  isAccessTokenExpired,
  setAuthToken,
} from '@/context/authRuntime'
import type { AuthTokenParams, CurrentAuthParams, LoginParams } from '@/types/authTypes'
import type { UserInfo } from '@/types/userTypes'

interface AuthContextValue {
  currentAuth?: CurrentAuthParams
  currentUserInfo?: UserInfo
  isLogin: boolean
  hasValidLogin: () => boolean
  clearLoginState: () => void
  loginAction: (params: LoginParams) => Promise<AuthTokenParams>
  registerAction: (params: CurrentAuthParams) => Promise<CurrentAuthParams>
  getAuthAction: () => Promise<CurrentAuthParams>
  refreshLoginStateAction: () => Promise<string>
  logoutAction: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [currentAuth, setCurrentAuth] = useState<CurrentAuthParams>()
  const [, setAuthVersion] = useState(0)

  const bumpAuthVersion = () => setAuthVersion((value) => value + 1)

  const clearLoginState = useCallback(() => {
    setCurrentAuth(undefined)
    clearStoredAuthInfo()
    bumpAuthVersion()
  }, [])

  const loginAction = useCallback(async (params: LoginParams) => {
    const tokenResult = await login(params)

    setAuthToken(tokenResult)
    bumpAuthVersion()

    return tokenResult
  }, [])

  const registerAction = useCallback(async (params: CurrentAuthParams) => register(params), [])

  const getAuthAction = useCallback(async () => {
    const auth = await getCurrentAuth()

    setCurrentAuth(auth)

    return auth
  }, [])

  const refreshLoginStateAction = useCallback(async () => {
    const accessToken = getAccessToken()

    if (accessToken && !isAccessTokenExpired()) {
      return accessToken
    }

    try {
      const tokenResult = await refreshAccessToken()

      setAuthToken(tokenResult)
      bumpAuthVersion()

      return tokenResult.accessToken
    } catch (error) {
      clearLoginState()

      throw error
    }
  }, [clearLoginState])

  const logoutAction = useCallback(async () => {
    try {
      if (getAccessToken()) {
        await logout()
      }
    } finally {
      clearLoginState()
    }
  }, [clearLoginState])

  const currentUserInfo = useMemo<UserInfo | undefined>(() => {
    if (!currentAuth) return undefined

    const displayName = currentAuth.name || currentAuth.phone || '用户'

    return {
      id: currentAuth.id || '',
      phone: currentAuth.phone,
      email: '',
      role: '',
      status: '',
      name: displayName,
    }
  }, [currentAuth])

  const value = useMemo<AuthContextValue>(
    () => ({
      currentAuth,
      currentUserInfo,
      isLogin: Boolean(getAccessToken()),
      hasValidLogin: () => Boolean(getAccessToken() && !isAccessTokenExpired()),
      clearLoginState,
      loginAction,
      registerAction,
      getAuthAction,
      refreshLoginStateAction,
      logoutAction,
    }),
    [
      currentAuth,
      currentUserInfo,
      clearLoginState,
      loginAction,
      registerAction,
      getAuthAction,
      refreshLoginStateAction,
      logoutAction,
    ],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}
