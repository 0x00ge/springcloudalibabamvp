import {get, post} from '@/utils/http/http.ts'

import type {UserForm, UserItem, UserPageConfig} from '@/types/types.ts'

// 获取用户管理页面配置：角色下拉、状态选项、默认表单值。
export const fetchUserPageConfig = () => get<UserPageConfig>('/users/config')

export const fetchUsers = (keyword = '') => get<UserItem[]>('/users', {keyword})

export const createUser = (data: UserForm) => post<UserItem>('/users/create', data)

export const updateUser = (id: number, data: UserForm) => post<UserItem>('/users/update', {id, ...data})

export const deleteUser = (id: number) => post<boolean>('/users/delete', {id})
