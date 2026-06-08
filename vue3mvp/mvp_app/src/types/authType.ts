export interface SendRegisterSmsCodeParams {
    phone: string
}

/**
 * 注册接口入参 DTO。
 * 字段和后端 CurrentAuthDTO 保持一致。
 */
export interface CurrentAuthDTO {
    name: string
    phone: string
    password: string
    confirmPassword: string
    smsCode: string
}

export interface LoginParams {
    phone: string
    password: string
}

/**
 * 当前鉴权用户基础信息。
 * 后端 CurrentAuthDTO 返回 id、name、phone 三个字段。
 */
export interface CurrentAuth {
    id: string
    name: string
    phone: string
}

/**
 * 后端登录/刷新 token 接口返回值。
 */
export interface TokenResult {
    /** 短期访问令牌，只保存在 Pinia/内存里，请求接口时放到 Authorization 请求头。 */
    accessToken: string
    /** 后端 DTO 中保留该字段；当前接口写入 HttpOnly Cookie 后会返回 null，前端不保存。 */
    refreshToken?: string | null
    /** accessToken 类型，通常为 Bearer。 */
    tokenType: string
    /** accessToken 过期秒数。 */
    accessTokenExpiresIn: number
    /** refreshToken Cookie 过期秒数，前端只用于调试或展示，不保存 refreshToken 原值。 */
    refreshTokenExpiresIn: number
}

/**
 * 刷新 accessToken 接口返回值。
 * 后端采用 HttpOnly Cookie refreshToken 轮换策略，响应体只返回新的 accessToken 信息。
 */
export type RefreshTokenResult = TokenResult
