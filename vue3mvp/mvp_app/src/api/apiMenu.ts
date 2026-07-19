import {del, get, post, put} from '@/utils/http/http.ts'

import type {MenuItem} from '@/types/layoutTypes.ts'
import type {MenuParams, MenuQuery} from '@/types/menuTypes.ts'
import type {PageQuery, PageResult} from '@/types/pageTypes.ts'

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

export const getMenuPage =
    async (query?: MenuQuery, pagination: PageQuery = {page: 1, size: 10}) => {
        const page = await get<PageResult<MenuItem>>('/menu/page', {
            page: pagination.page,
            size: pagination.size,
            title: query?.title || undefined,
            path: query?.path || undefined,
        })

        return {
            ...page,
            records: page.records.map(toMenuItem),
        }
    }

const toMenuDto =
    (data: MenuParams): Partial<MenuParams> => ({
        parentId: data.parentId || undefined,
        title: data.title,
        path: data.path,
        icon: data.icon || undefined,
        sortOrder: data.sortOrder ?? 0,
    })

export const createMenu =
    (data: MenuParams) => post<string>('/menu', toMenuDto(data))

export const updateMenu =
    (id: string, data: MenuParams) => put<void>('/menu/' + id, toMenuDto(data))

export const deleteMenu =
    (id: string) => del<void>('/menu/' + id)
