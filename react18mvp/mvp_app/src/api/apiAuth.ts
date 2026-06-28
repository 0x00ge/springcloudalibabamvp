import { get, post, postParams } from '@/utils/http/http'

import type {
  AuthTokenParams,
  CurrentAuthParams,
  LoginParams,
  SmsCodeByPhoneParams,
} from '@/types/authTypes'

export const registerCodeByPhone = (data: SmsCodeByPhoneParams) =>
  postParams<void>('/auth/register/code', {
    phone: data.phone,
  })

export const register = (data: CurrentAuthParams) =>
  post<CurrentAuthParams>('/auth/register', {
    id: data.id,
    phone: data.phone,
    password: data.password,
    confirmPassword: data.confirmPassword,
    smsCode: data.smsCode,
    name: data.name,
  })

export const login = (data: LoginParams) =>
  post<AuthTokenParams>('/auth/login', {
    phone: data.phone,
    password: data.password,
  })

export const getCurrentAuth = () => get<CurrentAuthParams>('/auth/me')

export const refreshAccessToken = () => postParams<AuthTokenParams>('/auth/refresh')

export const logout = () => postParams<void>('/auth/logout')
