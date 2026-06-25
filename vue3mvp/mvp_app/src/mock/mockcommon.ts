import Mock from 'mockjs'

import { BUSINESS_CODE } from '@/constants/httpCode.ts'
import type { OptionItem } from '@/types/types.ts'

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
