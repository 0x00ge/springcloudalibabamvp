import Mock from 'mockjs'

import { setupLayoutMock } from '@/mock/mocklayout.ts'
import { setupUserMock } from '@/mock/mockuser.ts'

Mock.setup({
  timeout: 200,
})

// mock 统一入口：每个业务模块只负责注册自己的接口，方便按文件查找和维护。
export const setupMock = () => {
  setupLayoutMock()
  setupUserMock()
}
