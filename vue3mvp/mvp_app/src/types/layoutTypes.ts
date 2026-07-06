/**
 * 通用下拉/单选/状态选项。
 * 用户管理里的角色、状态都可以复用这个结构。
 */
export interface OptionItem {
    /** 页面上展示给用户看的文字。 */
    label: string
    /** 真正提交给接口或用于匹配的值。 */
    value: string
    /** Element Plus 的 el-tag 类型，用来控制状态标签颜色。 */
    tagType?: '' | 'success' | 'info' | 'warning' | 'danger' | 'primary'
}

/**
 * 面包屑项。
 * 当前项目由路由 matched 动态生成，用于顶部栏展示当前位置。
 */
export interface BreadcrumbItem {
    /** 面包屑显示文字。 */
    title: string
    /** 可点击跳转的路径；没有 path 时只展示文字。 */
    path?: string
}

/**
 * 左侧菜单项。
 */
export interface MenuItem {
    /** 菜单唯一标识，用于 v-for 的 key。 */
    id: string
    /** 菜单显示名称。 */
    title: string
    /** 菜单跳转路径。 */
    path: string
    /** 菜单图标名称，和后端 t_menu.icon 对应。 */
    icon?: string
    /** 菜单排序值，越小越靠前。 */
    sort?: number
    /** 子菜单。 */
    children?: MenuItem[]
}
