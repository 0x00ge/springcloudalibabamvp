// 后端 Result<T>.code 使用的业务码。
export const BUSINESS_CODE = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
} as const

// HTTP 协议状态码，用于 error.response.status 这类场景。
export const HTTP_STATUS = {
  UNAUTHORIZED: 401,
} as const
