import { Button } from 'antd'
import { useNavigate } from 'react-router-dom'

import './NotFound.css'

export default function NotFound() {
  const navigate = useNavigate()

  // 回到后台首页，适合已经登录的用户从错误地址快速回到系统。
  const goHome = () => {
    navigate('/home')
  }

  // 返回上一页，适合用户只是手动输错或点错了链接。
  const goBack = () => {
    navigate(-1)
  }

  return (
    <main className="not-found-page">
      <section className="not-found-panel">
        <p className="status-code">404</p>
        <h1>页面不存在</h1>
        <p className="description">当前访问的地址不存在，可能是链接已失效或路径输入有误。</p>

        <div className="actions">
          <Button type="primary" onClick={goHome}>
            返回首页
          </Button>
          <Button onClick={goBack}>返回上一页</Button>
        </div>
      </section>
    </main>
  )
}
