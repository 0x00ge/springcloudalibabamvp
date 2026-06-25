import { get, post } from '@/utils/http/http.ts'

import type { DepartmentForm, DepartmentItem, DepartmentPageConfig } from '@/types/types.ts'

export const fetchDepartmentPageConfig = () => get<DepartmentPageConfig>('/departments/config')

export const fetchDepartments = (keyword = '') => get<DepartmentItem[]>('/departments', { keyword })

export const createDepartment = (data: DepartmentForm) =>
  post<DepartmentItem>('/departments/create', data)

export const updateDepartment = (id: number, data: DepartmentForm) =>
  post<DepartmentItem>('/departments/update', { id, ...data })

export const deleteDepartment = (id: number) => post<boolean>('/departments/delete', { id })
