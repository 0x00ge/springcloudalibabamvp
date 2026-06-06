import Mock from 'mockjs'

import { BUSINESS_CODE } from '@/constants/httpCode.ts'
import type { DepartmentItem, OptionItem } from '@/types/types.ts'

export interface MockRequestOptions {
  url: string
  body?: string
  headers?: Record<string, string>
}

export const success = <T>(data: T) => ({
  code: BUSINESS_CODE.SUCCESS,
  message: 'success',
  data,
})

export const unauthorized = (message = '登录已过期') => ({
  code: BUSINESS_CODE.UNAUTHORIZED,
  message,
  data: null,
})

export const parseBody = <T>(body?: string): T => (body ? JSON.parse(body) : ({} as T))

export const roleOptionsStore: OptionItem[] = Mock.mock<{ list: OptionItem[] }>({
  list: [
    { label: '超级管理员', value: '超级管理员' },
    { label: '运营管理员', value: '运营管理员' },
    { label: '普通用户', value: '普通用户' },
  ],
}).list

export const statusOptionsStore: OptionItem[] = Mock.mock<{ list: OptionItem[] }>({
  list: [
    { label: '启用', value: '启用', tagType: 'success' },
    { label: '停用', value: '停用', tagType: 'info' },
  ],
}).list

export const pickValue = (options: OptionItem[]) => Mock.Random.pick(options).value

// 部门数据会被 mockdepartment.ts 和 mockprofile.ts 共用，所以放在公共 mock 文件中维护同一份内存数据。
export const departmentStore: DepartmentItem[] = Mock.mock<{ list: DepartmentItem[] }>({
  'list|5': [
    {
      'id|+1': 1,
      name: '@pick(["技术部", "运营部", "产品部", "市场部", "财务部"])',
      leader: '@cname',
      memberCount: '@integer(6, 48)',
      status: () => pickValue(statusOptionsStore),
      description: '@ctitle(10, 24)',
    },
  ],
}).list
