import type { AuthTokenParams } from '@/types/authTypes'

const TOKEN_EXPIRE_BUFFER_SECONDS = 5

let accessTokenExpireTimer: number | undefined

const authToken: AuthTokenParams = {
  tokenType: '',
  accessToken: '',
  accessTokenExpiresIn: 0,
  refreshToken: undefined,
  refreshTokenExpiresIn: 0,
}

const clearAccessTokenExpireTimer = () => {
  if (accessTokenExpireTimer === undefined) return

  window.clearTimeout(accessTokenExpireTimer)
  accessTokenExpireTimer = undefined
}

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

const clearAccessToken = () => {
  clearAccessTokenExpireTimer()
  authToken.tokenType = ''
  authToken.accessToken = ''
  authToken.accessTokenExpiresIn = 0
}

export const setAuthToken = (tokens: AuthTokenParams) => {
  authToken.tokenType = tokens.tokenType || 'Bearer'
  authToken.accessToken = tokens.accessToken || ''
  authToken.accessTokenExpiresIn = tokens.accessTokenExpiresIn || 0
  authToken.refreshToken = undefined
  authToken.refreshTokenExpiresIn = tokens.refreshTokenExpiresIn || 0

  scheduleAccessTokenExpire(authToken.accessTokenExpiresIn)
}

export const clearStoredAuthInfo = () => {
  clearAccessToken()
  authToken.refreshToken = undefined
  authToken.refreshTokenExpiresIn = 0
}

export const getAccessToken = () => authToken.accessToken || undefined

export const isAccessTokenExpired = () => {
  if (!authToken.accessToken || authToken.accessTokenExpiresIn <= 0) return true

  return authToken.accessTokenExpiresIn <= TOKEN_EXPIRE_BUFFER_SECONDS
}
