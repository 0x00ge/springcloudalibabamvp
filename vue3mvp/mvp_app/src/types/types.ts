/**
 * 菜单图标名称。
 * mock 接口只返回字符串名称，菜单组件再根据名称映射成 Element Plus 图标组件。
 */
export type MenuIconName = 'Setting' | 'User' | 'UserFilled' | 'OfficeBuilding'

/**
 * 通用下拉/单选/状态选项。
 * 用户管理、部门管理、个人中心里的角色、部门、状态都可以复用这个结构。
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
 * 左侧菜单项。
 * 支持普通一级菜单，也支持带 children 的二级菜单。
 */
export interface MenuItem {
    /** 菜单唯一标识，用于 v-for 的 key。 */
    id: string
    /** 菜单显示名称。 */
    title: string
    /** 菜单跳转路径，通常和路由 path 保持一致。 */
    path: string
    /** 菜单图标名称，组件中会通过 iconMap 转成真实图标。 */
    icon?: MenuIconName
    /** 子菜单列表；存在 children 时渲染为 el-sub-menu。 */
    children?: MenuItem[]
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
 * 布局接口返回数据。
 * AppLayout 初始化时通过该结构拿到菜单和顶部栏用户信息。
 */
export interface LayoutMockData {
    /** 左侧菜单列表。 */
    menus: MenuItem[]
    /** 当前用户信息。 */
    user: UserInfo
}

/**
 * 用户管理表格中的单条用户数据。
 */
export interface UserItem {
    /** 用户唯一 id。 */
    id: number
    /** 用户名。 */
    name: string
    /** 用户角色。 */
    role: string
    /** 用户状态，例如启用、停用。 */
    status: string
    /** 用户邮箱。 */
    email: string
}

/**
 * 用户表单数据。
 * 新增/编辑弹窗不需要传 id，所以从 UserItem 中去掉 id。
 */
export type UserForm = Omit<UserItem, 'id'>

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
 * 页面初始化时从 /departments/config 获取状态选项和默认表单值。
 */
export interface DepartmentPageConfig {
    /** 部门状态选项。 */
    statusOptions: OptionItem[]
    /** 新增部门时的默认表单值。 */
    defaultForm: DepartmentForm
}

/**
 * 个人中心资料数据。
 * Profile.vue 展示和编辑时共用这一份结构。
 */
export interface ProfileData {
    /** 用户名。 */
    name: string
    /** 用户角色。 */
    role: string
    /** 所属部门。 */
    department: string
    /** 邮箱。 */
    email: string
    /** 手机号。 */
    phone: string
    /** 用户状态。 */
    status: string
    /** 头像文字。 */
    avatarText: string
}

/**
 * 个人中心页面配置。
 * 用于提供部门下拉选项和状态选项。
 */
export interface ProfilePageConfig {
    /** 部门下拉选项。 */
    departmentOptions: OptionItem[]
    /** 状态选项。 */
    statusOptions: OptionItem[]
}
