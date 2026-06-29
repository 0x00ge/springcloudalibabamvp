import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import AppLayout from '@/layout/AppLayout'
import Login from '@/pages/Login'
import NotFound from '@/pages/NotFound'
import User from '@/pages/User'
import ProtectedRoute from '@/router/ProtectedRoute'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/home" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<ProtectedRoute />}>
          <Route path="/home" element={<AppLayout />}>
            <Route index element={<Navigate to="/home/user" replace />} />
            <Route path="user" element={<User />} />
          </Route>
        </Route>
        <Route path="/404" element={<NotFound />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
