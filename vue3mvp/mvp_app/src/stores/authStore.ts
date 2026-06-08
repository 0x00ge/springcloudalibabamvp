import type { TokenResult } from '@/types/types.ts'

interface AuthRuntimeState {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: number
  refreshTokenExpiresAt: number
}

const authState: AuthRuntimeState = {
  accessToken: '',
  refreshToken: '',
  accessTokenExpiresAt: 0,
  refreshTokenExpiresAt: 0,
}

const secondsToExpiresAt = (seconds: number) => Date.now() + seconds * 1000

export const getAccessToken = () => authState.accessToken || undefined

export const getRefreshToken = () => authState.refreshToken || undefined

export const isAccessTokenExpired = () => !authState.accessToken || Date.now() >= authState.accessTokenExpiresAt

export const isRefreshTokenExpired = () => !authState.refreshToken || Date.now() >= authState.refreshTokenExpiresAt

export const setAuthTokens = (tokens: TokenResult) => {
  authState.accessToken = tokens.accessToken
  authState.refreshToken = tokens.refreshToken
  authState.accessTokenExpiresAt = secondsToExpiresAt(tokens.accessTokenExpiresIn)
  authState.refreshTokenExpiresAt = secondsToExpiresAt(tokens.refreshTokenExpiresIn)
}

export const clearStoredAuthInfo = () => {
  authState.accessToken = ''
  authState.refreshToken = ''
  authState.accessTokenExpiresAt = 0
  authState.refreshTokenExpiresAt = 0
}
