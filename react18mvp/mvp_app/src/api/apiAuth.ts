import { get, post, postParams } from '@/utils/http/http'

import type {
  AuthTokenParams,
  CurrentAuthParams,
  LoginParams,
  SmsCodeByPhoneParams,
} from '@/types/authTypes'

export const registerCodeByPhone = (data: SmsCodeByPhoneParams) => {
  const request: SmsCodeByPhoneParams = {
    phone: data.phone,
  }

  return postParams<void>('/auth/register/code', request)
}

export const register = (data: CurrentAuthParams) => {
  const request: CurrentAuthParams = {
    id: data.id,
    phone: data.phone,
    password: data.password,
    confirmPassword: data.confirmPassword,
    smsCode: data.smsCode,
    name: data.name,
  }

  return post<CurrentAuthParams>('/auth/register', request)
}

export const login = (data: LoginParams) => {
  const request: LoginParams = {
    phone: data.phone,
    password: data.password,
  }

  return post<AuthTokenParams>('/auth/login', request)
}

export const getCurrentAuth = () => get<CurrentAuthParams>('/auth/me')

export const refreshAccessToken = () => postParams<AuthTokenParams>('/auth/refresh')

export const logout = () => postParams<void>('/auth/logout')
