import {del, get, post, postParams, put} from '@/utils/http/http.ts'

import type { AuthTokenParams } from '@/types/authTypes.ts'
import type {
    LoginParams,
    UserForm,
    UserInfoConfig,
    UserParams,
    UserQuery,
} from '@/types/userTypes.ts'

interface UserDto {
    id?: string
    phone: string
    email?: string
    passwordHash?: string
    name: string
    permission?: 'ADMIN' | 'USER' | string
    status?: number
}

interface PageResult<T> {
    records: T[]
    total: number
    size: number
    current: number
}

const roleMap: Record<string, string> = {
    ADMIN: '管理员',
    USER: '普通用户',
}

const statusMap: Record<number, string> = {
    0: '禁用',
    1: '正常',
    2: '注销',
}

const roleValueMap: Record<string, string> = {
    管理员: 'ADMIN',
    普通用户: 'USER',
}

const statusValueMap: Record<string, number> = {
    禁用: 0,
    正常: 1,
    注销: 2,
}

const toUserParams =
    (user: UserDto): UserParams => ({
        id: user.id || '',
        name: user.name,
        phone: user.phone,
        permission: roleMap[user.permission || 'USER'] || user.permission || '普通用户',
        status: statusMap[user.status ?? 1] || '正常',
        email: user.email || '',
        passwordHash: user.passwordHash,
    })

const toUserDto =
    (data: UserForm): UserDto => ({
        name: data.name || '',
        phone: data.phone || '',
        email: data.email || undefined,
        passwordHash: data.passwordHash,
        permission: data.permission ? roleValueMap[data.permission] || data.permission : 'USER',
        status: data.status ? statusValueMap[data.status] ?? 1 : 1,
    })

// 发送注册短信验证码：
// 1. 后端会检查手机号格式和是否已注册。
// 2. 验证码保存到 Redis，当前后端用日志模拟短信发送。
export const registerCodeByPhone =
    (data: Pick<UserParams, 'phone'>) => {
        return postParams<void>('/auth/register/code', {
            phone: data.phone,
        })
    }

// 注册接口：
// 1. 注册前必须先调用 sendRegisterSmsCode 获取验证码。
// 2. 注册接口使用 CurrentAuthDTO JSON body 入参。
// 3. 注册成功后返回当前用户基础信息，前端再调用登录接口获取双 token。
export const register =
    (data: UserParams) => {
        const request: UserParams = {
            id: data.id,
            phone: data.phone,
            password: data.password,
            confirmPassword: data.confirmPassword,
            smsCode: data.smsCode,
            name: data.name,
        }

        return post<UserParams>('/auth/register', request)
    }

// 登录接口：
// 1. 用户输入账号密码后，Login.vue 会调用这个方法。
// 2. 后端校验成功后，响应体返回 accessToken，并通过 HttpOnly Cookie 写入 refreshToken。
// 3. 前端只保存 accessToken；业务接口只把 accessToken 放到 Authorization 请求头。
export const login =
    (data: LoginParams) => {
        const request: LoginParams = {
            phone: data.phone,
            password: data.password,
        }

        return post<AuthTokenParams>('/auth/login', request)
    }

// 当前登录用户：
// 1. 前端只携带 Authorization: Bearer accessToken。
// 2. Gateway 校验 token 后把用户 ID 透传成 X-User-Id。
// 3. user 服务读取 X-User-Id，并返回 CurrentAuthDTO。
export const getCurrentAuth =
    () => get<UserParams>('/auth/me')

// 刷新 accessToken：
// 1. accessToken 过期后，axios 拦截器会调用这个接口。
// 2. refreshToken 由浏览器自动携带 HttpOnly Cookie。
// 3. 后端轮换 refreshToken Cookie，响应体返回新的 accessToken。
export const refreshAccessToken =
    () => postParams<AuthTokenParams>('/auth/refresh')

// 登出接口：后端从 Authorization 请求头读取 accessToken，并从 Cookie 读取 refreshToken。
export const logout =
    () => postParams<void>('/auth/logout')

// 用户管理配置来自前端固定字典；用户列表和修改删除走真实后端 /user 接口。
export const getUserInfoConfig =
    async (): Promise<UserInfoConfig> => ({
        roleOptions: [
            {label: '管理员', value: '管理员', tagType: 'warning'},
            {label: '普通用户', value: '普通用户', tagType: 'info'},
        ],
        statusOptions: [
            {label: '正常', value: '正常', tagType: 'success'},
            {label: '禁用', value: '禁用', tagType: 'info'},
            {label: '注销', value: '注销', tagType: 'danger'},
        ],
        defaultUserForm: {
            name: '',
            phone: '',
            permission: '普通用户',
            status: '正常',
            email: '',
            passwordHash: '',
        },
    })

export const selectUsers =
    async (query?: Partial<UserQuery>) => {
        const page = await get<PageResult<UserDto>>('/user/page', {
            page: 1,
            size: 100,
            name: query?.name || undefined,
            phone: query?.phone || undefined,
            email: query?.email || undefined,
            permission: query?.permission ? roleValueMap[query.permission] || query.permission : undefined,
            status: query?.status ? statusValueMap[query.status] : undefined,
        })
        return page.records.map(toUserParams)
    }

export const createUser =
    async (data: UserForm) => {
        const id = await post<string>('/user', toUserDto(data))
        return {
            id,
            ...data,
            phone: data.phone || '',
        } as UserParams
    }

export const updateUser =
    async (id: string, data: UserForm) => {
        await put<void>('/user/' + id, toUserDto(data))
        return {
            id,
            ...data,
            phone: data.phone || '',
        } as UserParams
    }

export const deleteUser =
    (id: string) => del<void>('/user/' + id)
