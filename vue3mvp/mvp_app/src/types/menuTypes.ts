export interface MenuParams {
    parentId?: string
    title: string
    path: string
    icon?: string
    sortOrder?: number
}

export type MenuQuery = Partial<Pick<MenuParams, 'title' | 'path'>>
