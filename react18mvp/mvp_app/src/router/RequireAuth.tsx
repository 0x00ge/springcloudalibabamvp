import { useEffect, useState, type ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { useAuth } from '@/store/AuthContext'
import { getAccessToken } from '@/store/authStore'

interface RequireAuthProps {
  children: ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  const { clearLoginState, currentAuth, loadCurrentAuth, refreshLoginState } = useAuth()
  const location = useLocation()
  const [status, setStatus] = useState<'checking' | 'allowed' | 'denied'>('checking')

  useEffect(() => {
    let active = true

    const checkLogin = async () => {
      setStatus('checking')

      try {
        await refreshLoginState()

        if (!currentAuth) {
          await loadCurrentAuth()
        }

        if (active) setStatus('allowed')
      } catch {
        clearLoginState()
        if (active) setStatus('denied')
      }
    }

    checkLogin()

    return () => {
      active = false
    }
  }, [clearLoginState, currentAuth, loadCurrentAuth, location.pathname, location.search, refreshLoginState])

  if (status === 'checking') {
    return <div className="route-loading">加载中...</div>
  }

  if (status === 'denied') {
    return <Navigate to="/login" replace state={{ redirect: location.pathname + location.search }} />
  }

  return <>{children}</>
}

export function LoginRedirect({ children }: RequireAuthProps) {
  const location = useLocation()

  if (getAccessToken()) {
    const params = new URLSearchParams(location.search)
    return <Navigate to={params.get('redirect') || '/home'} replace />
  }

  return <>{children}</>
}
