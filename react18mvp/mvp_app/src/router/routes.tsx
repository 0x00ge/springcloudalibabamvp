import { Navigate, Route, Routes } from 'react-router-dom'

import { LoginRedirect, RequireAuth } from '@/router/RequireAuth'
import Home from '@/views/Home'
import Login from '@/views/Login'
import NotFound from '@/views/NotFound'
import User from '@/views/User'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/home" replace />} />
      <Route
        path="/login"
        element={
          <LoginRedirect>
            <Login />
          </LoginRedirect>
        }
      />
      <Route
        path="/home"
        element={
          <RequireAuth>
            <Home />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/home/user" replace />} />
        <Route path="user" element={<User />} />
      </Route>
      <Route path="/404" element={<NotFound />} />
      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  )
}
