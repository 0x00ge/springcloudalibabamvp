import {postParams} from '@/utils/http/http.ts'

import type {
    CurrentAuth,
    LoginParams,
    RefreshTokenResult,
    RegisterParams,
    SendRegisterSmsCodeParams,
    TokenResult,
} from '@/types/types.ts'


// 登录接口：
// 1. 用户输入账号密码后，Login.vue 会调用这个方法。
// 2. 后端校验成功后返回 accessToken 和 refreshToken。
// 3. 前端保存双 token；业务接口只把 accessToken 放到 Authorization 请求头。
export const login = (data: LoginParams) => {
    const request: LoginParams = {
        phone: data.phone,
        password: data.password,
    }

    return postParams<TokenResult>('/auth/login', request)
}

// 发送注册短信验证码：
// 1. 后端会检查手机号格式和是否已注册。
// 2. 验证码保存到 Redis，当前后端用日志模拟短信发送。
export const sendRegisterSmsCode = (data: SendRegisterSmsCodeParams) => {
    const request: SendRegisterSmsCodeParams = {
        phone: data.phone,
    }

    return postParams<void>('/auth/register/code', request)
}

// 注册接口：
// 1. 注册前必须先调用 sendRegisterSmsCode 获取验证码。
// 2. 注册成功后返回当前用户基础信息，前端再调用登录接口获取双 token。
export const register = (data: RegisterParams) => {
    const request: RegisterParams = {
        phone: data.phone,
        password: data.password,
        confirmPassword: data.confirmPassword,
        smsCode: data.smsCode,
        name: data.name,
    }

    return postParams<CurrentAuth>('/auth/register', request)
}

// 刷新 accessToken：
// 1. accessToken 过期后，axios 拦截器会调用这个接口。
// 2. refreshToken 通过请求参数传给后端。
// 3. 后端轮换双 token，前端保存新的 accessToken 和 refreshToken。
export const refreshAccessToken = (refreshToken: string) =>
    postParams<RefreshTokenResult>('/auth/refresh', {refreshToken})

// 登出接口：后端从 Authorization 请求头读取 accessToken，并通过 refreshToken 删除刷新白名单。
export const logout = (refreshToken: string) => postParams<void>('/auth/logout', {refreshToken})
