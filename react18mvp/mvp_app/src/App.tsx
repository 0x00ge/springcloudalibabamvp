import { BrowserRouter } from 'react-router-dom'

import { NoticeHost } from '@/components/NoticeHost'
import { AppRoutes } from '@/router/routes'
import { AuthProvider } from '@/store/AuthContext'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
        <NoticeHost />
      </BrowserRouter>
    </AuthProvider>
  )
}
