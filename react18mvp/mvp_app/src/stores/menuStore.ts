import { useSyncExternalStore } from 'react'

import type { MenuItem } from '@/types/layoutTypes'

const STORAGE_KEY = 'react18mvp.menus'

export const defaultMenus: MenuItem[] = [
  {
    id: 'system',
    title: '系统管理',
    path: '/home/system',
    sort: 1,
    locked: true,
    children: [
      {
        id: 'user',
        title: '用户管理',
        path: '/home/user',
        sort: 1,
        locked: true,
      },
      {
        id: 'menu',
        title: '菜单管理',
        path: '/home/menu',
        sort: 2,
        locked: true,
      },
    ],
  },
]

export type MenuFormValues = Pick<MenuItem, 'title' | 'path' | 'sort'>

const listeners = new Set<() => void>()

const cloneMenus = (menus: MenuItem[]): MenuItem[] => menus.map((menu) => ({
  ...menu,
  children: menu.children ? cloneMenus(menu.children) : undefined,
}))

const sortMenus = (menus: MenuItem[]): MenuItem[] =>
  [...menus]
    .sort((left, right) => (left.sort ?? 0) - (right.sort ?? 0))
    .map((menu) => ({
      ...menu,
      children: menu.children?.length ? sortMenus(menu.children) : undefined,
    }))

const readMenusFromStorage = (): MenuItem[] => {
  if (typeof window === 'undefined') return cloneMenus(defaultMenus)

  try {
    const rawMenus = window.localStorage.getItem(STORAGE_KEY)
    if (!rawMenus) return cloneMenus(defaultMenus)

    return JSON.parse(rawMenus) as MenuItem[]
  } catch {
    return cloneMenus(defaultMenus)
  }
}

let menus = sortMenus(readMenusFromStorage())
let snapshot = cloneMenus(menus)

const emitChange = () => {
  snapshot = cloneMenus(menus)
  listeners.forEach((listener) => listener())
}

const persistMenus = () => {
  if (typeof window === 'undefined') return

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(menus))
}

const subscribe = (listener: () => void) => {
  listeners.add(listener)

  return () => {
    listeners.delete(listener)
  }
}

const updateMenus = (nextMenus: MenuItem[]) => {
  menus = sortMenus(nextMenus)
  persistMenus()
  emitChange()
}

const createMenuId = () => {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `menu-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const appendChildMenu = (items: MenuItem[], parentId: string, childMenu: MenuItem[]): MenuItem[] =>
  items.map((item) => {
    if (item.id === parentId) {
      return {
        ...item,
        children: sortMenus([...(item.children || []), ...childMenu]),
      }
    }

    return {
      ...item,
      children: item.children ? appendChildMenu(item.children, parentId, childMenu) : undefined,
    }
  })

const updateMenuItem = (items: MenuItem[], id: string, values: MenuFormValues): MenuItem[] =>
  items.map((item) => {
    if (item.id === id) {
      return {
        ...item,
        ...values,
      }
    }

    return {
      ...item,
      children: item.children ? updateMenuItem(item.children, id, values) : undefined,
    }
  })

const deleteMenuItem = (items: MenuItem[], id: string): MenuItem[] =>
  items
    .filter((item) => item.id !== id)
    .map((item) => ({
      ...item,
      children: item.children ? deleteMenuItem(item.children, id) : undefined,
    }))

export const menuStore = {
  getSnapshot: () => snapshot,

  addRootMenu: (values: MenuFormValues) => {
    updateMenus([
      ...menus,
      {
        id: createMenuId(),
        ...values,
      },
    ])
  },

  addChildMenu: (parentId: string, values: MenuFormValues) => {
    updateMenus(
      appendChildMenu(menus, parentId, [
        {
          id: createMenuId(),
          ...values,
        },
      ]),
    )
  },

  updateMenu: (id: string, values: MenuFormValues) => {
    updateMenus(updateMenuItem(menus, id, values))
  },

  deleteMenu: (id: string) => {
    updateMenus(deleteMenuItem(menus, id))
  },

  resetMenus: () => {
    updateMenus(cloneMenus(defaultMenus))
  },
}

export function useMenuSnapshot() {
  return useSyncExternalStore(subscribe, menuStore.getSnapshot, menuStore.getSnapshot)
}
