import { get, post } from '@/utils/http/http.ts'

import type { ProfileData, ProfilePageConfig } from '@/types/types.ts'

// 获取个人中心页面配置：部门下拉、状态选项等动态字典。
export const fetchProfilePageConfig = () => get<ProfilePageConfig>('/profile/config')

export const fetchProfile = () => get<ProfileData>('/profile')

export const updateProfile = (data: ProfileData) => post<ProfileData>('/profile/update', data)
