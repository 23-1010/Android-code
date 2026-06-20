export const formatTime = (date: Date) => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return (
    [year, month, day].map(formatNumber).join('/') +
    ' ' +
    [hour, minute, second].map(formatNumber).join(':')
  )
}

const formatNumber = (n: number) => {
  const s = n.toString()
  return s[1] ? s : '0' + s
}

// 封装 wx.request，自动添加 ngrok 跳过警告头
export const request = (options: WechatMiniprogram.RequestOption) => {
  return wx.request({
    ...options,
    header: {
      'ngrok-skip-browser-warning': 'true',
      ...(options.header || {})
    }
  })
}
