type NoticeType = 'success' | 'warning' | 'error' | 'info'

const titleMap: Record<NoticeType, string> = {
  success: '成功',
  warning: '提示',
  error: '错误',
  info: '提示',
}

export const notify = (message: string, type: NoticeType = 'info') => {
  if (type === 'error') {
    console.error(`${titleMap[type]}: ${message}`)
  }

  window.dispatchEvent(
    new CustomEvent('mvp-notice', {
      detail: {
        id: crypto.randomUUID(),
        title: titleMap[type],
        message,
        type,
      },
    }),
  )
}
