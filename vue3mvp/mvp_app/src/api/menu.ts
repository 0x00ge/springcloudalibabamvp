import {get} from '@/utils/http/http.ts'

import type {MenuItem} from '@/types/types.ts'

// 查询当前登录用户可见菜单树，侧边栏直接使用这个真实接口。
export const userMenuTree =
    () => get<MenuItem[]>('/user/menu/tree')
