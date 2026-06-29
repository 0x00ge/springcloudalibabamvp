import { useNavigate } from 'react-router-dom'
import { HomeOutlined, RollbackOutlined } from '@ant-design/icons'
import { Button, Space } from 'antd'

export default function NotFound() {
  const navigate = useNavigate()

  return (
    <main className="not-found-page">
      <section className="not-found-panel">
        <p className="status-code">404</p>
        <h1>页面不存在</h1>
        <p className="description">当前访问的地址不存在，可能是链接已失效或路径输入有误。</p>

        <Space className="actions" wrap>
          <Button type="primary" icon={<HomeOutlined />} onClick={() => navigate('/home')}>
            返回首页
          </Button>
          <Button icon={<RollbackOutlined />} onClick={() => navigate(-1)}>
            返回上一页
          </Button>
        </Space>
      </section>
    </main>
  )
}
