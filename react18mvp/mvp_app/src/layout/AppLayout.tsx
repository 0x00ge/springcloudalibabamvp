import { Layout, message, Modal } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { matchPath, useLocation, useMatches, useNavigate } from 'react-router-dom'

import AppMain from '@/layout/components/AppMain'
import AppMenu from '@/layout/components/AppMenu'
import AppTopbar from '@/layout/components/AppTopbar'
import { authStore, useAuthSnapshot } from '@/stores/authStore'
import { useMenuSnapshot } from '@/stores/menuStore'
import type { BreadcrumbItem } from '@/types/layoutTypes'
import './AppLayout.css'

export default function AppLayout() {
  const [loading, setLoading] = useState(false)
  const [logoutLoading, setLogoutLoading] = useState(false)

  const location = useLocation()
  const navigate = useNavigate()
  const matches = useMatches()
  const auth = useAuthSnapshot()
  const menus = useMenuSnapshot()

  const breadcrumbs = useMemo<BreadcrumbItem[]>(
    () =>
      matches.reduce<BreadcrumbItem[]>((items, routeMatch) => {
          const handle = routeMatch.handle as { title?: string } | undefined

          if (!handle?.title) return items

          items.push({
            title: handle.title,
            path: matchPath(routeMatch.pathname, location.pathname) ? routeMatch.pathname : undefined,
          })

          return items
        }, []),
    [location.pathname, matches],
  )

  const handleLogout = async () => {
    const confirmed = await new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '退出登录',
        content: '确定退出当前账号吗？',
        okText: '退出',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })

    if (!confirmed) return

    setLogoutLoading(true)

    try {
      await authStore.logoutAction()
      message.success('已退出登录')
    } catch {
      message.warning('已清理本地登录状态')
    } finally {
      setLogoutLoading(false)
      navigate('/login', { replace: true })
    }
  }

  useEffect(() => {
    let ignore = false

    setLoading(true)
    authStore
      .getAuthAction()
      .catch(() => undefined)
      .finally(() => {
        if (!ignore) {
          setLoading(false)
        }
      })

    return () => {
      ignore = true
    }
  }, [])

  return (
    /* 布局 */
    <Layout className="app-layout">
      {/* 侧边 */}
      <Layout.Sider className="app-aside" width={200}>
        <AppMenu menus={menus} />
      </Layout.Sider>

      <Layout className="app-body">
        {/* 顶部 */}
        <AppTopbar
          breadcrumbs={breadcrumbs}
          user={loading || logoutLoading ? { ...auth.currentUserInfo, name: 'Loading' } : auth.currentUserInfo}
          onLogout={handleLogout}
        />
        {/* 主体 */}
        <AppMain />
      </Layout>
    </Layout>
  )
}
