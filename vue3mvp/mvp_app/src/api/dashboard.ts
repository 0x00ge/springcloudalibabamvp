import { get } from '@/utils/http/http.ts'

import type { LayoutMockData } from '@/types/types.ts'

// 获取布局基础数据：菜单、面包屑、当前用户。
export const fetchLayoutData = () => get<LayoutMockData>('/layout')
