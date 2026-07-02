import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import 'antd/dist/reset.css'
// 使用你定义的全局清零样式，其他视觉和布局样式放在具体组件中维护。
import './style.css'

import { router } from '@/router'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
)
