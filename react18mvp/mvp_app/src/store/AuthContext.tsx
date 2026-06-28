import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

import {
  clearStoredAuthInfo,
  getAuthAction,
  loginAction,
  logoutAction,
  refreshLoginStateAction,
  registerAction,
} from '@/store/authStore'
import type { CurrentAuthParams, LoginParams } from '@/types/authTypes'
import type { UserInfo } from '@/types/userTypes'

interface AuthContextValue {
  currentAuth?: CurrentAuthParams
  currentUserInfo?: UserInfo
  clearLoginState: () => void
  login: (params: LoginParams) => Promise<void>
  register: (params: CurrentAuthParams) => Promise<void>
  loadCurrentAuth: () => Promise<CurrentAuthParams>
  refreshLoginState: () => Promise<string>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const toCurrentUserInfo = (currentAuth?: CurrentAuthParams): UserInfo | undefined => {
  if (!currentAuth) return undefined

  const displayName = currentAuth.name || currentAuth.phone || '用户'

  return {
    userId: currentAuth.id,
    phone: currentAuth.phone,
    name: displayName,
    avatarText: displayName.slice(0, 1).toUpperCase(),
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentAuth, setCurrentAuth] = useState<CurrentAuthParams>()

  const clearLoginState = useCallback(() => {
    setCurrentAuth(undefined)
    clearStoredAuthInfo()
  }, [])

  const login = useCallback(async (params: LoginParams) => {
    await loginAction(params)
  }, [])

  const register = useCallback(async (params: CurrentAuthParams) => {
    await registerAction(params)
  }, [])

  const loadCurrentAuth = useCallback(async () => {
    const auth = await getAuthAction()
    setCurrentAuth(auth)
    return auth
  }, [])

  const refreshLoginState = useCallback(async () => {
    try {
      return await refreshLoginStateAction()
    } catch (error) {
      setCurrentAuth(undefined)
      throw error
    }
  }, [])

  const logout = useCallback(async () => {
    await logoutAction()
    setCurrentAuth(undefined)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      currentAuth,
      currentUserInfo: toCurrentUserInfo(currentAuth),
      clearLoginState,
      login,
      register,
      loadCurrentAuth,
      refreshLoginState,
      logout,
    }),
    [clearLoginState, currentAuth, loadCurrentAuth, login, logout, refreshLoginState, register],
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
