import Mock from 'mockjs'

import {
  departmentStore,
  parseBody,
  pickValue,
  roleOptionsStore,
  statusOptionsStore,
  success,
  type MockRequestOptions,
} from '@/mock/mockcommon.ts'
import type { ProfileData, ProfilePageConfig } from '@/types/types.ts'

let profileStore: ProfileData = Mock.mock<ProfileData>({
  name: '@pick(["Admin", "Manager", "Operator"])',
  role: () => pickValue(roleOptionsStore),
  department: () => Mock.Random.pick(departmentStore).name,
  email: '@email',
  phone: /^1[3-9]\d{9}$/,
  status: () => pickValue(statusOptionsStore),
  avatarText: '@pick(["A", "M", "O"])',
})

export const createProfilePageConfigMock = (): ProfilePageConfig =>
  Mock.mock<ProfilePageConfig>({
    departmentOptions: departmentStore.map((department) => ({
      label: department.name,
      value: department.name,
    })),
    statusOptions: statusOptionsStore,
  })

// Profile.vue 对应的个人中心 mock 接口。
export const setupProfileMock = () => {
  Mock.mock('/api/profile/config', 'get', () => success(createProfilePageConfigMock()))
  Mock.mock('/api/profile', 'get', () => success(profileStore))
  Mock.mock('/api/profile/update', 'post', (options: MockRequestOptions) => {
    profileStore = parseBody<ProfileData>(options.body)

    return success(profileStore)
  })
}
