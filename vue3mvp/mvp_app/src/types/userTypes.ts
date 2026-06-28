import type { OptionItem } from '@/types/types.ts'

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
 * 用户列表查询条件。
 */
export interface UserQuery {
    name: string
    phone: string
    email: string
    role: string
    status: string
}

/**
 * 用户管理页面配置。
 * 页面初始化时从 /users/config 获取，避免角色、状态和默认表单写死在组件里。
 */
export interface UserInfoConfig {
    /** 角色下拉选项。 */
    roleOptions: OptionItem[]
    /** 状态选项，表格状态 tag 颜色也从这里取。 */
    statusOptions: OptionItem[]
    /** 新增用户时的默认表单值。 */
    defaultForm: UserForm
}
