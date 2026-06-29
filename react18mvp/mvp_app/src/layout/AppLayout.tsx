import { useEffect, useMemo, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  BellOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
} from '@ant-design/icons'
import {
  Avatar,
  Breadcrumb,
  Button,
  Dropdown,
  Layout,
  Menu,
  Modal,
  Skeleton,
  Space,
  type MenuProps,
} from 'antd'

import { useAuth } from '@/context/AuthContext'
import type { BreadcrumbItem, MenuItem } from '@/types/common'

const { Header, Sider, Content } = Layout

const menus: MenuItem[] = [
  { id: 'user', title: '用户管理', path: '/home/user', icon: 'UserOutlined' },
]

const iconMap = {
  UserOutlined: <UserOutlined />,
}

const menuItems: MenuProps['items'] = menus.map((menu) => ({
  key: menu.path,
  icon: menu.icon ? iconMap[menu.icon] : undefined,
  label: menu.title,
}))

const getBreadcrumbs = (pathname: string): BreadcrumbItem[] => {
  const items: BreadcrumbItem[] = [{ title: '首页', path: '/home' }]

  if (pathname.startsWith('/home/user')) {
    items.push({ title: '用户管理', path: '/home/user' })
  }

  return items
}

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [loading, setLoading] = useState(false)
  const [logoutLoading, setLogoutLoading] = useState(false)

  const location = useLocation()
  const navigate = useNavigate()
  const auth = useAuth()

  const breadcrumbs = useMemo(() => getBreadcrumbs(location.pathname), [location.pathname])

  useEffect(() => {
    let ignore = false

    const loadCurrentAuth = async () => {
      setLoading(true)

      try {
        await auth.getAuthAction()
      } finally {
        if (!ignore) {
          setLoading(false)
        }
      }
    }

    if (!auth.currentAuth) {
      loadCurrentAuth()
    }

    return () => {
      ignore = true
    }
  }, [auth])

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(String(key))
  }

  const handleLogout = () => {
    Modal.confirm({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      okText: '退出',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setLogoutLoading(true)

        try {
          await auth.logoutAction()
        } finally {
          setLogoutLoading(false)
          navigate('/login', { replace: true })
        }
      },
    })
  }

  const currentUser = auth.currentUserInfo

  return (
    <Layout className="app-layout">
      <Sider className="app-sider" width={220} collapsedWidth={64} collapsed={collapsed}>
        <div className={`brand ${collapsed ? 'brand-collapsed' : ''}`}>
          <MenuUnfoldOutlined className="brand-icon" />
          {!collapsed && <span className="brand-title">React18 MVP</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>

      <Layout className="app-body">
        <Header className="app-header">
          <div className="header-left">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed((value) => !value)}
            />
            {loading ? (
              <Skeleton.Input className="breadcrumb-loading" active size="small" />
            ) : (
              <Breadcrumb items={breadcrumbs.map((item) => ({ title: item.title }))} />
            )}
          </div>

          <div className="header-right">
            <Button type="text" shape="circle" icon={<BellOutlined />} />
            <Dropdown
              menu={{
                items: [{ key: 'logout', icon: <LogoutOutlined />, label: '退出登录' }],
                onClick: handleLogout,
              }}
            >
              <Space className="user-entry">
                <Avatar size={32}>{currentUser?.name?.slice(0, 1).toUpperCase() || '-'}</Avatar>
                <span>{loading || logoutLoading ? 'Loading' : currentUser?.name || '用户'}</span>
              </Space>
            </Dropdown>
          </div>
        </Header>

        <Content className="app-main">
          <section className="content-panel">
            <Outlet />
          </section>
        </Content>
      </Layout>
    </Layout>
  )
}
