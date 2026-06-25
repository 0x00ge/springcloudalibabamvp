import Mock from 'mockjs'

import {
  departmentStore,
  parseBody,
  pickValue,
  statusOptionsStore,
  success,
  type MockRequestOptions,
} from '@/mock/mockcommon.ts'
import type { DepartmentForm, DepartmentItem, DepartmentPageConfig } from '@/types/types.ts'

export const createDepartmentPageConfigMock = (): DepartmentPageConfig =>
  Mock.mock<DepartmentPageConfig>({
    statusOptions: statusOptionsStore,
    defaultForm: {
      name: '',
      leader: '',
      memberCount: 0,
      status: () => pickValue(statusOptionsStore),
      description: '',
    },
  })

export const queryDepartmentsMock = (keyword = '') => {
  const value = keyword.trim().toLowerCase()

  if (!value) return departmentStore

  return departmentStore.filter(
    (department) =>
      department.name.toLowerCase().includes(value) ||
      department.leader.toLowerCase().includes(value) ||
      department.status.toLowerCase().includes(value),
  )
}

export const createDepartmentMock = (data: DepartmentForm) => {
  const department = {
    id: Mock.Random.integer(1000, 999999),
    ...data,
  }

  departmentStore.unshift(department)

  return department
}

export const updateDepartmentMock = (data: DepartmentItem) => {
  const index = departmentStore.findIndex((department) => department.id === data.id)

  if (index > -1) {
    departmentStore[index] = data
  }

  return data
}

export const deleteDepartmentMock = (id: number) => {
  const index = departmentStore.findIndex((department) => department.id === id)

  if (index > -1) {
    departmentStore.splice(index, 1)
  }

  return true
}

export const setupDepartmentMock = () => {
  Mock.mock('/api/departments/config', 'get', () => success(createDepartmentPageConfigMock()))
  Mock.mock(/\/api\/departments(?:\?.*)?$/, 'get', (options: MockRequestOptions) => {
    const url = new URL(options.url, window.location.origin)

    return success(queryDepartmentsMock(url.searchParams.get('keyword') || ''))
  })
  Mock.mock('/api/departments/create', 'post', (options: MockRequestOptions) =>
    success(createDepartmentMock(parseBody<DepartmentForm>(options.body))),
  )
  Mock.mock('/api/departments/update', 'post', (options: MockRequestOptions) =>
    success(updateDepartmentMock(parseBody<DepartmentItem>(options.body))),
  )
  Mock.mock('/api/departments/delete', 'post', (options: MockRequestOptions) => {
    const { id } = parseBody<{ id: number }>(options.body)

    return success(deleteDepartmentMock(id))
  })
}
