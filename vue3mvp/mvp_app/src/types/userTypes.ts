/**
 * 登录请求参数。
 */
export interface LoginParams {
    /** 登录手机号。 */
    phone: string
    /** 登录密码。 */
    password: string
}

/**
 * 用户接口参数。
 * 注册参数、当前登录用户返回值都复用这个结构；token 仍然只放在 authTypes.ts。
 */
export interface UserParams {
    /** 用户唯一 id，注册时可为空，当前登录用户会返回。 */
    id?: string
    /** 用户名，注册和当前登录用户展示会使用。 */
    name?: string
    /** 手机号。 */
    phone: string
    /** 用户权限标识。 */
    permission?: 'ADMIN' | 'USER' | string
    /** 用户邮箱。 */
    email?: string
    /** 用户状态展示值，例如正常、禁用、注销。 */
    status?: number
    /** 后端返回的密码哈希，编辑提交时保持原值。 */
    passwordHash?: string
    /** 注册或新增用户时提交的密码。 */
    password?: string
    /** 注册时提交的确认密码。 */
    confirmPassword?: string
    /** 注册时提交的短信验证码。 */
    smsCode?: string
}

/**
 * 用户表单数据。
 * 新增/编辑弹窗不需要传 id，所以从 UserParams 中选取可编辑字段。
 */
export type UserForm = Pick<UserParams, 'name' | 'phone' | 'email' | 'status' | 'passwordHash' | 'permission'>

/**
 * 用户列表查询条件。
 */
export type UserQuery = Pick<UserParams, 'name' | 'phone' | 'email' | 'status' | 'permission'>

/**
 * 用户管理下拉选项。
 */
export interface OptionItem {
    /** 接口或用于匹配的值。 */
    value: string
    /** Element Plus 的 el-tag 类型，用来控制状态标签颜色。 */
    tagType?: '' | 'success' | 'info' | 'warning' | 'danger' | 'primary'
}

/**
 * 用户管理页面配置。
 * 页面初始化时从 /users/config 获取，避免角色、状态和默认表单写死在组件里。
 */
export interface UserConfig {
    /** 角色下拉选项。 */
    roleOptions: OptionItem[]
    /** 状态选项，表格状态 tag 颜色也从这里取。 */
    statusOptions: OptionItem[]
}
