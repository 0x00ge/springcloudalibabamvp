import {del, get, post, postParams, put} from '@/utils/http/http.ts'

import type { MenuItem } from '@/types/layoutTypes.ts'
import type {MenuForm} from "@/types/appMenuTypes.ts";

const toMenuItem =
    (menu: MenuItem): MenuItem => ({
        id: menu.id,
        parentId: menu.parentId,
        title: menu.title,
        path: menu.path,
        icon: menu.icon,
        sortOrder: menu.sortOrder ?? 0,
        children: menu.children?.length ? menu.children.map(toMenuItem) : undefined,
    })

export const getMenuTree =
    async () => {
        const menus = await get<MenuItem[]>('/menu/tree')

        return menus.map(toMenuItem)
    }

export const resetMenuTree =
    async () => {
        const menus = await postParams<MenuItem[]>('/menu/reset')

        return menus.map(toMenuItem)
    }

const toMenuDto =
    (data: MenuForm): Partial<MenuItem> => ({
        parentId: data.parentId || undefined,
        title: data.title,
        path: data.path,
        icon: data.icon || undefined,
        sortOrder: data.sortOrder ?? 0,
    })

export const createMenu =
    (data: MenuForm) => post<string>('/menu', toMenuDto(data))

export const updateMenu =
    (id: string, data: MenuForm) => put<void>('/menu/' + id, toMenuDto(data))

export const deleteMenu =
    (id: string) => del<void>('/menu/' + id)
