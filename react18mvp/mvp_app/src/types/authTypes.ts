export interface SmsCodeByPhoneParams {
  phone: string
}

export interface CurrentAuthParams {
  id?: string
  name: string
  phone: string
  permission?: 'ADMIN' | 'USER' | string
  password: string
  confirmPassword: string
  smsCode: string
}

export interface LoginParams {
  phone: string
  password: string
}

export interface AuthTokenParams {
  tokenType: string
  accessToken: string
  accessTokenExpiresIn: number
  refreshToken?: string | null
  refreshTokenExpiresIn: number
}
