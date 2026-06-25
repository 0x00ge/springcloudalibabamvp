import {get, post, put, del} from '@/utils/http/http.ts'

import type {MenuForm, MenuItem} from '@/types/types.ts'

// 查询当前登录用户可见菜单树，侧边栏直接使用这个真实接口。
export const userMenuTree =
    () => get<MenuItem[]>('/user/menu/tree')

// 管理端查询菜单树。后端会校验当前登录用户是否为 ADMIN。
export const userMenuTreeCheck =
    (userId?: string) => get<MenuItem[]>('/user/menu/tree/check', {userId})

export const createUserMenu =
    (data: MenuForm) => post<string>('/user/menu', data)

// 修改菜单。PUT 当前只在菜单管理使用，所以直接走 axiosInstance，复用统一鉴权拦截器。
export const updateUserMenu =
    (value: string, data: MenuForm) => put<string>('/user/menu/' + data)

// 删除菜单。后端会软删除当前菜单及其子菜单。
export const deleteUserMenu =
  (id: string) => del<string>('/user/menu/' + id)
