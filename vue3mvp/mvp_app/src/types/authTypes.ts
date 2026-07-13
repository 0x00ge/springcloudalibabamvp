export interface SmsCodeByPhoneParams {
    phone: string
}

export interface AuthParams {
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
    type: string
    /** 短期访问令牌，只保存在 Pinia/内存里，请求接口时放到 Authorization 请求头。 */
    accessToken: string
    /** accessToken 过期秒数。 */
    accessTokenExpiresIn: number
}
