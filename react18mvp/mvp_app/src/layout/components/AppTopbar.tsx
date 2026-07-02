import { Avatar, Breadcrumb, Dropdown, Layout } from 'antd'
import type { MenuProps } from 'antd'

import type { BreadcrumbItem } from '@/types/layoutTypes'
import type { UserInfo } from '@/types/userTypes'
import './AppTopbar.css'

interface AppTopbarProps {
  breadcrumbs: BreadcrumbItem[]
  user: UserInfo
  onLogout: () => void
}

export default function AppTopbar({ breadcrumbs, user, onLogout }: AppTopbarProps) {
  const dropdownItems: MenuProps['items'] = [
    {
      key: 'logout',
      label: '退出登录',
      onClick: onLogout,
    },
  ]

  return (
    <Layout.Header className="app-header">
      <div className="header-left">
        {/* 面包屑 */}
        <Breadcrumb
          items={breadcrumbs.map((item) => ({
            title: item.title,
            href: item.path,
          }))}
        />
      </div>

      <div className="header-right">
        {/* 下拉菜单 */}
        <Dropdown menu={{ items: dropdownItems }} trigger={['click']}>
          <div className="user-entry" onClick={(event) => event.preventDefault()}>
            <Avatar size={32}>{user?.name?.slice(0, 1).toUpperCase() || '-'}</Avatar>
            <div>{user?.name || 'Loading'}</div>
          </div>
        </Dropdown>
      </div>
    </Layout.Header>
  )
}
