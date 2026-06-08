import { get, post, postParams } from '@/utils/http/http.ts'

import type {
    CurrentAuth,
    CurrentAuthDTO,
    LoginParams,
    RefreshTokenResult,
    SendRegisterSmsCodeParams,
    TokenResult,
} from '@/types/authType.ts'

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
// 2. 注册接口使用 CurrentAuthDTO JSON body 入参。
// 3. 注册成功后返回当前用户基础信息，前端再调用登录接口获取双 token。
export const register = (data: CurrentAuthDTO) => {
    const request: CurrentAuthDTO = {
        phone: data.phone,
        password: data.password,
        confirmPassword: data.confirmPassword,
        smsCode: data.smsCode,
        name: data.name,
    }

    return post<CurrentAuth>('/auth/register', request)
}

// 登录接口：
// 1. 用户输入账号密码后，Login.vue 会调用这个方法。
// 2. 后端校验成功后，响应体返回 accessToken，并通过 HttpOnly Cookie 写入 refreshToken。
// 3. 前端只保存 accessToken；业务接口只把 accessToken 放到 Authorization 请求头。
export const login = (data: LoginParams) => {
    const request: LoginParams = {
        phone: data.phone,
        password: data.password,
    }

    return postParams<TokenResult>('/auth/login', request)
}

// 当前登录用户：
// 1. 前端只携带 Authorization: Bearer accessToken。
// 2. Gateway 校验 token 后把用户 ID 透传成 X-User-Id。
// 3. user 服务读取 X-User-Id，并返回 CurrentAuthDTO。
export const getCurrentAuth = () => get<CurrentAuth>('/auth/me')

// 刷新 accessToken：
// 1. accessToken 过期后，axios 拦截器会调用这个接口。
// 2. refreshToken 由浏览器自动携带 HttpOnly Cookie。
// 3. 后端轮换 refreshToken Cookie，响应体返回新的 accessToken。
export const refreshAccessToken = () => postParams<RefreshTokenResult>('/auth/refresh')

// 登出接口：后端从 Authorization 请求头读取 accessToken，并从 Cookie 读取 refreshToken。
export const logout = () => postParams<void>('/auth/logout')
