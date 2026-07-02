import { Menu } from 'antd'
import type { ItemType } from 'antd/es/menu/interface'
import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import type { MenuItem } from '@/types/layoutTypes'
import './AppMenu.css'

interface AppMenuProps {
  menus: MenuItem[]
}

function flattenMenus(menus: MenuItem[], map = new Map<string, MenuItem>()) {
  menus.forEach((menu) => {
    map.set(menu.id, menu)
    if (menu.children?.length) {
      flattenMenus(menu.children, map)
    }
  })

  return map
}

function findMenuIdByPath(menus: MenuItem[], path: string): string | undefined {
  for (const menu of menus) {
    if (menu.path === path) return menu.id

    const childKey = menu.children?.length ? findMenuIdByPath(menu.children, path) : undefined
    if (childKey) return childKey
  }

  return undefined
}

function renderMenuItems(menus: MenuItem[]): ItemType[] {
  return menus.map((menu) => ({
    key: menu.id,
    label: menu.title,
    children: menu.children?.length ? renderMenuItems(menu.children) : undefined,
  }))
}

export default function AppMenu({ menus }: AppMenuProps) {
  const location = useLocation()
  const navigate = useNavigate()

  const menuMap = useMemo(() => flattenMenus(menus), [menus])
  const items = useMemo(() => renderMenuItems(menus), [menus])
  const selectedKey = findMenuIdByPath(menus, location.pathname)

  return (
    <>
      {/* 品牌区。 */}
      <div className="brand">
        <span className="brand-title">React18 MVP</span>
      </div>

      {/*
        Ant Design 菜单：
        - selectedKeys 跟随当前路由路径高亮
        - 点击普通菜单时使用 react-router 跳转
        - 菜单项由 AppMenu 内部递归生成，支持任意层级子菜单
      */}
      <Menu
        className="side-menu"
        mode="inline"
        selectedKeys={selectedKey ? [selectedKey] : []}
        items={items}
        onClick={({ key }) => {
          const menu = menuMap.get(String(key))
          if (menu?.path) {
            navigate(menu.path)
          }
        }}
      />
    </>
  )
}
