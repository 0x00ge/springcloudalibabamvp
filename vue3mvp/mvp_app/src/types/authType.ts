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
    /** accessToken 类型，通常为 Bearer。 */
    tokenType: string
    /** 短期访问令牌，只保存在 Pinia/内存里，请求接口时放到 Authorization 请求头。 */
    accessToken: string
    /** accessToken 过期秒数。 */
    accessTokenExpiresIn: number
    /** 后端 DTO 中保留该字段；当前接口写入 HttpOnly Cookie 后会返回 null，前端不保存。 */
    refreshToken?: string | null
    /** refreshToken Cookie 过期秒数，前端只用于调试或展示，不保存 refreshToken 原值。 */
    refreshTokenExpiresIn: number
}
