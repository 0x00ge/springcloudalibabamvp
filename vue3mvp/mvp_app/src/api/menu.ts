import axiosInstance from '@/utils/http/axios.ts'
import { get, post } from '@/utils/http/http.ts'

import type { MenuForm, MenuItem } from '@/types/types.ts'

// 查询当前登录用户可见菜单树，侧边栏直接使用这个真实接口。
export const userMenuTree = () => get<MenuItem[]>('/user/menu/tree')

// 管理端查询菜单树。后端会校验当前登录用户是否为 ADMIN。
export const userMenuTreeCheck = (userId?: string) => get<MenuItem[]>('/user/menu/tree/check', { userId })

// 新增菜单，数据写入 t_user_menu。
export const createMenu = (data: MenuForm) => post<string>('/user/menu', data)

// 修改菜单。PUT 当前只在菜单管理使用，所以直接走 axiosInstance，复用统一鉴权拦截器。
export const updateMenu = async (id: string, data: MenuForm) => {
  const response = await axiosInstance.put('/user/menu/' + id, data)

  return response.data.data as void
}

// 删除菜单。后端会软删除当前菜单及其子菜单。
export const deleteMenu = async (id: string) => {
  const response = await axiosInstance.delete('/user/menu/' + id)

  return response.data.data as void
}
