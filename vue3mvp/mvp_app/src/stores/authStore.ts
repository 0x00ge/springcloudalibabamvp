import type { TokenResult } from '@/types/authType.ts'

interface AuthRuntimeState {
  accessToken: string
  accessTokenExpiresAt: number
}

const authState: AuthRuntimeState = {
  accessToken: '',
  accessTokenExpiresAt: 0,
}

const secondsToExpiresAt = (seconds: number) => Date.now() + seconds * 1000

export const getAccessToken = () => authState.accessToken || undefined

export const isAccessTokenExpired = () => !authState.accessToken || Date.now() >= authState.accessTokenExpiresAt

export const setAuthTokens = (tokens: TokenResult) => {
  authState.accessToken = tokens.accessToken
  authState.accessTokenExpiresAt = secondsToExpiresAt(tokens.accessTokenExpiresIn)
}

export const clearStoredAuthInfo = () => {
  authState.accessToken = ''
  authState.accessTokenExpiresAt = 0
}
