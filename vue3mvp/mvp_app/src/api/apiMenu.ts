import {del, get, post, postParams, put} from '@/utils/http/http.ts'

import type { MenuItem } from '@/types/layoutTypes.ts'

export interface MenuForm {
    parentId?: string
    title: string
    path: string
    icon?: string
    sortOrder?: number
}

interface MenuDto {
    id: string
    parentId?: string
    title: string
    path: string
    icon?: string
    level?: number
    sortOrder?: number
    userId?: string
    children?: MenuDto[]
}

const toMenuItem =
    (menu: MenuDto): MenuItem => ({
        id: menu.id,
        parentId: menu.parentId,
        title: menu.title,
        path: menu.path,
        icon: menu.icon,
        sort: menu.sortOrder ?? 0,
        children: menu.children?.length ? menu.children.map(toMenuItem) : undefined,
    })

export const getMenuTree =
    async () => {
        const menus = await get<MenuDto[]>('/menu/tree')

        return menus.map(toMenuItem)
    }

export const resetMenuTree =
    async () => {
        const menus = await postParams<MenuDto[]>('/menu/reset')

        return menus.map(toMenuItem)
    }

const toMenuDto =
    (data: MenuForm): Partial<MenuDto> => ({
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
