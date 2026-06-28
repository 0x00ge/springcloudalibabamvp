import { useEffect, useState } from 'react'

type NoticeType = 'success' | 'warning' | 'error' | 'info'

interface NoticeItem {
  id: string
  title: string
  message: string
  type: NoticeType
}

export function NoticeHost() {
  const [notices, setNotices] = useState<NoticeItem[]>([])

  useEffect(() => {
    const handleNotice = (event: Event) => {
      const notice = (event as CustomEvent<NoticeItem>).detail

      setNotices((current) => [...current, notice])

      window.setTimeout(() => {
        setNotices((current) => current.filter((item) => item.id !== notice.id))
      }, 2600)
    }

    window.addEventListener('mvp-notice', handleNotice)

    return () => window.removeEventListener('mvp-notice', handleNotice)
  }, [])

  return (
    <div className="notice-host" aria-live="polite">
      {notices.map((notice) => (
        <div className={`notice notice-${notice.type}`} key={notice.id}>
          <strong>{notice.title}</strong>
          <span>{notice.message}</span>
        </div>
      ))}
    </div>
  )
}
