import {post} from '@/utils/http/http.ts'

import type {
    LoginParams,
    RefreshTokenResult,
    RegisterParams,
    SendRegisterSmsCodeParams,
    TokenResult,
} from '@/types/types.ts'


// 登录接口：
// 1. 用户输入账号密码后，Login.vue 会调用这个方法。
// 2. 后端校验成功后返回 accessToken，并通过 HttpOnly Cookie 写入 refreshToken。
// 3. 前端只保存 accessToken；refreshToken 由浏览器 Cookie 自动保存。
export const login = (data: LoginParams) => {
    const request: LoginParams = {
        username: data.username,
        password: data.password,
    }

    return post<TokenResult>('/auth/login', request)
}

// 发送注册短信验证码：
// 1. 后端会检查手机号格式和是否已注册。
// 2. 验证码保存到 Redis，当前后端用日志模拟短信发送。
export const sendRegisterSmsCode = (data: SendRegisterSmsCodeParams) => {
    const request: SendRegisterSmsCodeParams = {
        phone: data.phone,
    }

    return post<void>('/auth/register/code', request)
}

// 注册接口：
// 1. 注册前必须先调用 sendRegisterSmsCode 获取验证码。
// 2. 注册成功后和登录一样返回 accessToken，并写入 refreshToken Cookie。
export const register = (data: RegisterParams) => {
    const request: RegisterParams = {
        phone: data.phone,
        password: data.password,
        confirmPassword: data.confirmPassword,
        smsCode: data.smsCode,
        name: data.name,
    }

    return post<TokenResult>('/auth/register', request)
}

// 刷新 accessToken：
// 1. accessToken 过期后，axios 拦截器会调用这个接口。
// 2. refreshToken 由 HttpOnly Cookie 自动携带，前端不传 body。
// 3. 后端会轮换 refreshToken Cookie，前端只保存新的 accessToken。
export const refreshAccessToken = () => post<RefreshTokenResult>('/auth/refresh')

// 登出接口：后端从 Authorization 请求头读取 accessToken，并拉黑当前 token。
export const logout = () => post<void>('/auth/logout')
