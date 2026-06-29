import type { OptionItem } from '@/types/common'

export interface UserInfo {
  id: string
  name: string
  phone: string
  email: string
  role: string
  status: string
  passwordHash?: string
  deviceId?: string
}

export type UserForm = Pick<UserInfo, 'name' | 'phone' | 'email' | 'role' | 'status' | 'passwordHash'>

export type UserQuery = Pick<UserForm, 'name' | 'phone' | 'email' | 'role' | 'status'>

export interface UserInfoConfig {
  roleOptions: OptionItem[]
  statusOptions: OptionItem[]
  defaultUserForm: UserForm
}
