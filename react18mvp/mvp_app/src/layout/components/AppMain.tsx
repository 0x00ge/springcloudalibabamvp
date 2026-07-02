import { Layout } from 'antd'
import { Outlet } from 'react-router-dom'

import './AppMain.css'

export default function AppMain() {
  return (
    <Layout.Content className="app-main">
      <section className="content-panel">
        <Outlet />
      </section>
    </Layout.Content>
  )
}
