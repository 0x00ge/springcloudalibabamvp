import {del, get, post, put} from '@/utils/http/http.ts'

import type {UserForm, UserItem, UserPageConfig} from '@/types/userTypes.ts'

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

const toUserItem =
    (user: UserDto): UserItem => ({
        id: user.id || '',
        name: user.name,
        phone: user.phone,
        role: roleMap[user.permission || 'USER'] || user.permission || '普通用户',
        status: statusMap[user.status ?? 1] || '正常',
        email: user.email || '',
        passwordHash: user.passwordHash,
    })

const toUserDto =
    (data: UserForm): UserDto => ({
        name: data.name,
        phone: data.phone,
        email: data.email || undefined,
        passwordHash: data.passwordHash,
        permission: roleValueMap[data.role] || data.role || 'USER',
        status: statusValueMap[data.status] ?? 1,
    })

// 用户管理配置来自前端固定字典；用户列表和修改删除走真实后端 /user 接口。
export const fetchUserPageConfig =
    async (): Promise<UserPageConfig> => ({
        roleOptions: [
            {label: '管理员', value: '管理员', tagType: 'warning'},
            {label: '普通用户', value: '普通用户', tagType: 'info'},
        ],
        statusOptions: [
            {label: '正常', value: '正常', tagType: 'success'},
            {label: '禁用', value: '禁用', tagType: 'info'},
            {label: '注销', value: '注销', tagType: 'danger'},
        ],
        defaultForm: {
            name: '',
            phone: '',
            role: '普通用户',
            status: '正常',
            email: '',
            passwordHash: '',
        },
    })

export const selectUsers =
    async () => {
        const page = await get<PageResult<UserDto>>('/user/page', {page: 1, size: 100})
        return page.records.map(toUserItem)
    }

export const createUser =
    async (data: UserForm) => {
        const id = await post<string>('/user', toUserDto(data))
        return {
            id,
            ...data,
        } as UserItem
    }

export const updateUser =
    async (id: string, data: UserForm) => {
        await put<void>('/user/' + id, toUserDto(data))
        return {
            id,
            ...data,
        } as UserItem
    }

export const deleteUser =
    (id: string) => del<void>('/user/' + id)
