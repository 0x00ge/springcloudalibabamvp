import { Navigate, createBrowserRouter, redirect } from 'react-router-dom'

import AppLayout from '@/layout/AppLayout'
import { authStore, getAccessToken } from '@/stores/authStore'
import Login from '@/views/Login'
import NotFound from '@/views/NotFound'
import User from '@/views/User'

async function loginLoader({ request }: { request: Request }) {
  if (getAccessToken()) {
    const url = new URL(request.url)

    return redirect(url.searchParams.get('redirect') || '/home')
  }

  authStore.clearLoginState()

  return null
}

async function protectedLoader({ request }: { request: Request }) {
  try {
    await authStore.refreshLoginStateAction()

    return null
  } catch {
    authStore.clearLoginState()

    const url = new URL(request.url)

    return redirect(`/login?redirect=${encodeURIComponent(url.pathname + url.search + url.hash)}`)
  }
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/home" replace />,
  },
  {
    path: '/login',
    element: <Login />,
    loader: loginLoader,
  },
  {
    path: '/home',
    element: <AppLayout />,
    loader: protectedLoader,
    handle: {
      title: '首页',
    },
    children: [
      {
        index: true,
        element: <Navigate to="/home/user" replace />,
      },
      {
        path: 'user',
        element: <User />,
        handle: {
          title: '用户管理',
        },
      },
    ],
  },
  {
    path: '/404',
    element: <NotFound />,
  },
  {
    path: '*',
    element: <Navigate to="/404" replace />,
  },
])
