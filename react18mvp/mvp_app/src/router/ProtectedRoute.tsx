import { useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Spin } from 'antd'

import { useAuth } from '@/context/AuthContext'
import { getAccessToken } from '@/context/authRuntime'

export default function ProtectedRoute() {
  const auth = useAuth()
  const location = useLocation()
  const [checking, setChecking] = useState(true)
  const [authorized, setAuthorized] = useState(false)

  useEffect(() => {
    let ignore = false

    const checkAuth = async () => {
      setChecking(true)

      try {
        await auth.refreshLoginStateAction()

        if (!auth.currentAuth) {
          await auth.getAuthAction()
        }

        if (!ignore) {
          setAuthorized(true)
        }
      } catch {
        auth.clearLoginState()

        if (!ignore) {
          setAuthorized(false)
        }
      } finally {
        if (!ignore) {
          setChecking(false)
        }
      }
    }

    checkAuth()

    return () => {
      ignore = true
    }
  }, [auth, location.pathname])

  if (checking) {
    return (
      <div className="route-loading">
        <Spin tip="正在恢复登录态" />
      </div>
    )
  }

  if (!authorized || !getAccessToken()) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />
  }

  return <Outlet />
}
