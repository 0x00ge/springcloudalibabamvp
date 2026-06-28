import { useEffect, useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Bell,
  ChevronDown,
  LogOut,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  UserRound,
} from 'lucide-react'

import { useAuth } from '@/store/AuthContext'
import type { BreadcrumbItem, MenuItem } from '@/types/types'
import { notify } from '@/utils/notify'

const menus: MenuItem[] = [
  { id: 'user', title: '用户管理', path: '/home/user', icon: 'UserFilled' },
]

const breadcrumbTitleMap: Record<string, string> = {
  '/home': '首页',
  '/home/user': '用户管理',
}

export default function LayoutApp() {
  const [isCollapse, setIsCollapse] = useState(false)
  const [loading, setLoading] = useState(false)
  const [logoutLoading, setLogoutLoading] = useState(false)
  const [dropdownOpen, setDropdownOpen] = useState(false)

  const auth = useAuth()
  const { clearLoginState, loadCurrentAuth, logout } = auth
  const location = useLocation()
  const navigate = useNavigate()

  const asideWidth = isCollapse ? 64 : 220

  const breadcrumbs = useMemo<BreadcrumbItem[]>(() => {
    const paths = ['/home']

    if (location.pathname !== '/home') {
      paths.push(location.pathname)
    }

    return paths
      .filter((path) => breadcrumbTitleMap[path])
      .map((path) => ({
        title: breadcrumbTitleMap[path],
        path,
      }))
  }, [location.pathname])

  useEffect(() => {
    let active = true

    const loadAuth = async () => {
      setLoading(true)

      try {
        await loadCurrentAuth()
      } finally {
        if (active) setLoading(false)
      }
    }

    loadAuth()

    return () => {
      active = false
    }
  }, [loadCurrentAuth])

  const handleLogout = async () => {
    const confirmed = window.confirm('确定退出当前账号吗？')

    if (!confirmed) return

    setLogoutLoading(true)

    try {
      await logout()
      notify('已退出登录', 'success')
    } catch {
      clearLoginState()
      notify('已清理本地登录状态', 'warning')
    } finally {
      setLogoutLoading(false)
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="app-layout">
      <aside className="app-aside" style={{ width: asideWidth }}>
        <div className={`brand ${isCollapse ? 'brand-collapsed' : ''}`}>
          <Menu size={22} />
          {!isCollapse && <span className="brand-title">React18 MVP</span>}
        </div>

        <nav className={`side-menu ${isCollapse ? 'side-menu-collapsed' : ''}`}>
          {menus.map((menu) => (
            <Link
              className={`menu-item ${location.pathname === menu.path ? 'menu-item-active' : ''}`}
              key={menu.id}
              to={menu.path}
              title={isCollapse ? menu.title : undefined}
            >
              <UserRound size={18} />
              {!isCollapse && <span>{menu.title}</span>}
            </Link>
          ))}
        </nav>
      </aside>

      <div className="app-body">
        <header className="app-header">
          <div className="header-left">
            <button
              className="icon-button collapse-button"
              onClick={() => setIsCollapse((current) => !current)}
              type="button"
              title={isCollapse ? '展开侧栏' : '收起侧栏'}
            >
              {isCollapse ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />}
            </button>

            {loading ? (
              <div className="breadcrumb-loading" />
            ) : (
              <nav className="breadcrumb">
                {breadcrumbs.map((item, index) => (
                  <span className="breadcrumb-item" key={item.title}>
                    {index > 0 && <span className="breadcrumb-separator">/</span>}
                    {item.path ? <Link to={item.path}>{item.title}</Link> : item.title}
                  </span>
                ))}
              </nav>
            )}
          </div>

          <div className="header-right">
            <button className="icon-button" type="button" title="通知">
              <Bell size={18} />
            </button>

            <div className="user-dropdown">
              <button
                className="user-entry"
                disabled={loading || logoutLoading}
                onClick={() => setDropdownOpen((current) => !current)}
                type="button"
              >
                <span className="avatar">{auth.currentUserInfo?.avatarText || '-'}</span>
                <span>{auth.currentUserInfo?.name || 'Loading'}</span>
                <ChevronDown size={16} />
              </button>

              {dropdownOpen && (
                <div className="dropdown-menu">
                  <button type="button" onClick={handleLogout}>
                    <LogOut size={16} />
                    <span>退出登录</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="app-main">
          <section className="content-panel">
            <Outlet />
          </section>
        </main>
      </div>
    </div>
  )
}
