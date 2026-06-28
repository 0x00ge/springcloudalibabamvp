import type { OptionItem } from '@/types/types'

export interface UserInfo {
  userId?: string
  phone?: string
  deviceId?: string
  name: string
  avatarText: string
}

export interface UserItem {
  id: string
  name: string
  phone: string
  role: string
  status: string
  email: string
  passwordHash?: string
}

export interface UserForm {
  name: string
  phone: string
  email: string
  role: string
  status: string
  passwordHash?: string
}

export interface UserPageConfig {
  roleOptions: OptionItem[]
  statusOptions: OptionItem[]
  defaultForm: UserForm
}
