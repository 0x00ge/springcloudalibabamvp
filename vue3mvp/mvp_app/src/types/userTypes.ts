import type { OptionItem } from '@/types/types.ts'

/**
 * 用户基础信息。
 * 登录用户展示、用户管理表格、新增编辑回显都会使用。
 */
export interface UserInfo {
    /** 用户唯一 id。 */
    id: string
    /** 用户名。 */
    name: string
    /** 手机号。 */
    phone: string
    /** 用户邮箱。 */
    email: string
    /** 角色。 */
    role: string
    /** 用户状态，例如正常、禁用、注销。 */
    status: string
    /** 后端返回的密码哈希，编辑提交时保持原值。 */
    passwordHash?: string
    /** 当前设备标识，仅当前登录用户有值。 */
    deviceId?: string
}

/**
 * 用户表单数据。
 * 新增/编辑弹窗不需要传 id，所以从 UserInfo 中选取可编辑字段。
 */
export type UserForm = Pick<UserInfo, 'name' | 'phone' | 'email' | 'role' | 'status' | 'passwordHash'>

/**
 * 用户列表查询条件。
 */
export type UserQuery = Pick<UserForm, 'name' | 'phone' | 'email' | 'role' | 'status'>

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
    defaultUserForm: UserForm
}
