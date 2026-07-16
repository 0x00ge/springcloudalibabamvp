import {del, get, post, put} from '@/utils/http/http.ts'

import type {MenuItem} from '@/types/layoutTypes.ts'

const toMenuItem =
    (menu: MenuItem): MenuItem => ({
        id: menu.id,
        parentId: menu.parentId,
        title: menu.title,
        path: menu.path,
        icon: menu.icon,
        sortOrder: menu.sortOrder ?? 0,
        createdAt: menu.createdAt,
        children: menu.children?.length ? menu.children.map(toMenuItem) : undefined,
    })

export const getMenuTree =
    async () => {
        const menus = await get<MenuItem[]>('/menu/tree')

        return menus.map(toMenuItem)
    }

const toMenuDto =
    (data: MenuItem): Partial<MenuItem> => ({
        parentId: data.parentId || undefined,
        title: data.title,
        path: data.path,
        icon: data.icon || undefined,
        sortOrder: data.sortOrder ?? 0,
    })

export const createMenu =
    (data: MenuItem) => post<string>('/menu', toMenuDto(data))

export const updateMenu =
    (id: string, data: MenuItem) => put<void>('/menu/' + id, toMenuDto(data))

export const deleteMenu =
    (id: string) => del<void>('/menu/' + id)
