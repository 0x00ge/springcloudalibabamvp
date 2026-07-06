import { get, postParams } from '@/utils/http/http.ts'

import type { MenuItem } from '@/types/layoutTypes.ts'

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
