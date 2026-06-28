export interface OptionItem {
  label: string
  value: string
  tagType?: '' | 'success' | 'info' | 'warning' | 'danger' | 'primary'
}

export interface BreadcrumbItem {
  title: string
  path?: string
}

export type MenuIconName = 'UserFilled'

export interface MenuItem {
  id: string
  title: string
  path: string
  icon?: MenuIconName
  children?: MenuItem[]
}
