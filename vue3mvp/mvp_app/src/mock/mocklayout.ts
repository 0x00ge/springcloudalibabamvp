import Mock from 'mockjs'

import { success } from '@/mock/mockcommon.ts'
import type { LayoutMockData } from '@/types/types.ts'

export const createLayoutMock = (): LayoutMockData =>
  Mock.mock({
    menus: [
      {
        id: '@guid',
        title: '系统管理',
        path: '/system',
        icon: 'Setting',
        children: [
          { id: '@guid', title: '用户管理', path: '/home/user', icon: 'UserFilled' },
          { id: '@guid', title: '部门管理', path: '/home/department', icon: 'OfficeBuilding' },
        ],
      },
    ],
    user: {
      name: '@pick(["Admin", "Manager", "Operator"])',
      avatarText: '@pick(["A", "M", "O"])',
    },
  })

// Home.vue/AppLayout.vue 对应的布局 mock 接口。
export const setupLayoutMock = () => {
  Mock.mock('/api/layout', 'get', () => success(createLayoutMock()))
}
