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
 * 当前登录用户基础信息。
 * 登录接口、布局接口、顶部栏头像区域都会使用。
 */
export interface UserInfo {
    /** 后端用户 id。 */
    userId?: string
    /** 手机号。 */
    phone?: string
    /** 当前设备标识。 */
    deviceId?: string
    /** 用户名称。 */
    name: string
    /** 头像内显示的简短文字，例如 A、M、O。 */
    avatarText: string
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
    /** 菜单图标名称。 */
    icon?: MenuIconName
    /** 子菜单。 */
    children?: MenuItem[]
}

/**
 * 用户管理表格中的单条用户数据。
 */
export interface UserItem {
    /** 用户唯一 id。 */
    id: string
    /** 用户名。 */
    name: string
    /** 手机号。 */
    phone: string
    /** 角色。 */
    role: string
    /** 用户状态，例如启用、停用。 */
    status: string
    /** 用户邮箱。 */
    email: string
    /** 后端返回的密码哈希，编辑提交时保持原值。 */
    passwordHash?: string
}

/**
 * 用户表单数据。
 * 新增/编辑弹窗不需要传 id，所以从 UserItem 中去掉 id。
 */
export interface UserForm {
    name: string
    phone: string
    email: string
    role: string
    status: string
    passwordHash?: string
}

/**
 * 用户管理页面配置。
 * 页面初始化时从 /users/config 获取，避免角色、状态和默认表单写死在组件里。
 */
export interface UserPageConfig {
    /** 角色下拉选项。 */
    roleOptions: OptionItem[]
    /** 状态选项，表格状态 tag 颜色也从这里取。 */
    statusOptions: OptionItem[]
    /** 新增用户时的默认表单值。 */
    defaultForm: UserForm
}

/**
 * 部门管理表格中的单条部门数据。
 */
export interface DepartmentItem {
    /** 部门唯一 id。 */
    id: number
    /** 部门名称。 */
    name: string
    /** 部门负责人。 */
    leader: string
    /** 部门成员数量。 */
    memberCount: number
    /** 部门状态，例如启用、停用。 */
    status: string
    /** 部门说明。 */
    description: string
}

/**
 * 部门表单数据。
 * 新增/编辑弹窗不需要传 id，所以从 DepartmentItem 中去掉 id。
 */
export type DepartmentForm = Omit<DepartmentItem, 'id'>

/**
 * 部门管理页面配置。
 */
export interface DepartmentPageConfig {
    /** 部门状态选项。 */
    statusOptions: OptionItem[]
    /** 新增部门时的默认表单值。 */
    defaultForm: DepartmentForm
}
/**
 * 菜单图标名称。
 * 侧边栏当前只保留用户管理和部门管理两个入口。
 */
export type MenuIconName = 'UserFilled' | 'OfficeBuilding'
