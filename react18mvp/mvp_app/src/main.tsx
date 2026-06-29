import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'

import { AuthProvider } from '@/context/AuthContext'
import AppRouter from '@/router/AppRouter'
import '@/style.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </ConfigProvider>
  </React.StrictMode>,
)
