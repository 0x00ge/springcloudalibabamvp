export interface OptionItem {
  label: string
  value: string
  tagType?: '' | 'success' | 'processing' | 'error' | 'warning' | 'default'
}

export interface BreadcrumbItem {
  title: string
  path?: string
}

export interface MenuItem {
  id: string
  title: string
  path: string
  icon?: 'UserOutlined'
  children?: MenuItem[]
}
