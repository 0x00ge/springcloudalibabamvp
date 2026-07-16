/**
 * 左侧菜单项
 */
export interface MenuItem {
    /** 菜单唯一标识，用于 v-for 的 key。 */
    id: string
    /** 父菜单 id，顶级菜单为空。 */
    parentId?: string
    /** 菜单显示名称。 */
    title: string
    /** 菜单跳转路径。 */
    path: string
    /** 菜单图标名称，和后端 t_menu.icon 对应。 */
    icon?: string
    /** 菜单排序值，越小越靠前。 */
    sortOrder?: number
    createdAt?: string
    /** 子菜单。 */
    children?: MenuItem[]
}

/**
 * 面包屑项
 */
export interface BreadcrumbItem {
    /** 面包屑显示文字。 */
    title: string
    /** 可点击跳转的路径；没有 path 时只展示文字。 */
    path?: string
}
