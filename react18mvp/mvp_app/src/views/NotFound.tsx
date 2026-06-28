import { Home, RotateCcw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export default function NotFound() {
  const navigate = useNavigate()

  return (
    <main className="not-found-page">
      <section className="not-found-panel">
        <p className="status-code">404</p>
        <h1>页面不存在</h1>
        <p className="description">当前访问的地址不存在，可能是链接已失效或路径输入有误。</p>

        <div className="actions">
          <button className="button button-primary" onClick={() => navigate('/home')} type="button">
            <Home size={16} />
            返回首页
          </button>
          <button className="button" onClick={() => navigate(-1)} type="button">
            <RotateCcw size={16} />
            返回上一页
          </button>
        </div>
      </section>
    </main>
  )
}
