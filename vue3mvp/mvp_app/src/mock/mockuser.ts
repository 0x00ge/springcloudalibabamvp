import Mock from 'mockjs'

import {
  parseBody,
  pickValue,
  roleOptionsStore,
  statusOptionsStore,
  success,
  type MockRequestOptions,
} from '@/mock/mockcommon.ts'
import type { UserForm, UserItem, UserPageConfig } from '@/types/types.ts'

// mockUserList 模拟后端数据库中的用户表数据，只在 mock 接口内部使用。
// 它不是 Pinia 的全局 userStore，真实前端登录状态放在 src/stores/userStore.ts。
const mockUserList: UserItem[] = Mock.mock<{ list: UserItem[] }>({
  'list|6': [
    {
      'id|+1': 1,
      name: '@pick(["Admin", "Manager", "Operator", "Editor", "Auditor", "Guest"])',
      role: () => pickValue(roleOptionsStore),
      status: () => pickValue(statusOptionsStore),
      email: '@email',
    },
  ],
}).list

export const createUserPageConfigMock = (): UserPageConfig =>
  Mock.mock<UserPageConfig>({
    roleOptions: roleOptionsStore,
    statusOptions: statusOptionsStore,
    defaultForm: {
      name: '',
      role: () => roleOptionsStore.at(-1)?.value || '',
      status: () => statusOptionsStore[0]?.value || '',
      email: '',
    },
  })

export const queryUsersMock = (keyword = '') => {
  const value = keyword.trim().toLowerCase()

  if (!value) return mockUserList

  return mockUserList.filter(
    (user) =>
      user.name.toLowerCase().includes(value) ||
      user.email.toLowerCase().includes(value) ||
      user.role.toLowerCase().includes(value),
  )
}

export const createUserMock = (data: UserForm) => {
  const user = {
    id: Mock.Random.integer(1000, 999999),
    ...data,
  }

  mockUserList.unshift(user)

  return user
}

export const updateUserMock = (data: UserItem) => {
  const index = mockUserList.findIndex((user) => user.id === data.id)

  if (index > -1) {
    mockUserList[index] = data
  }

  return data
}

export const deleteUserMock = (id: number) => {
  const index = mockUserList.findIndex((user) => user.id === id)

  if (index > -1) {
    mockUserList.splice(index, 1)
  }

  return true
}

// User.vue 对应的用户管理 mock 接口。
export const setupUserMock = () => {
  Mock.mock('/api/users/config', 'get', () => success(createUserPageConfigMock()))
  Mock.mock(/\/api\/users(?:\?.*)?$/, 'get', (options: MockRequestOptions) => {
    const url = new URL(options.url, window.location.origin)

    return success(queryUsersMock(url.searchParams.get('keyword') || ''))
  })
  Mock.mock('/api/users/create', 'post', (options: MockRequestOptions) =>
    success(createUserMock(parseBody<UserForm>(options.body))),
  )
  Mock.mock('/api/users/update', 'post', (options: MockRequestOptions) =>
    success(updateUserMock(parseBody<UserItem>(options.body))),
  )
  Mock.mock('/api/users/delete', 'post', (options: MockRequestOptions) => {
    const { id } = parseBody<{ id: number }>(options.body)

    return success(deleteUserMock(id))
  })
}
