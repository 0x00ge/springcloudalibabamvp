import { get, post } from '@/utils/http/http.ts'

import type { DepartmentForm, DepartmentItem, DepartmentPageConfig } from '@/types/types.ts'

// 获取部门管理页面配置：状态选项和新增时的默认表单值。
export const fetchDepartmentPageConfig = () => get<DepartmentPageConfig>('/departments/config')

// 获取部门列表，keyword 会传给 MockJS 接口做简单筛选。
export const fetchDepartments = (keyword = '') => get<DepartmentItem[]>('/departments', { keyword })

// 新增部门，真实项目中这里可以无缝替换成后端接口。
export const createDepartment = (data: DepartmentForm) =>
  post<DepartmentItem>('/departments/create', data)

// 修改部门，通过 id 定位要更新的数据。
export const updateDepartment = (id: number, data: DepartmentForm) =>
  post<DepartmentItem>('/departments/update', { id, ...data })

// 删除部门，MockJS 会同步更新内存中的部门列表。
export const deleteDepartment = (id: number) => post<boolean>('/departments/delete', { id })
