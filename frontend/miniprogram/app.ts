import { request } from './utils/util';

App({
  onLaunch() {
    // ── 先写入/修复本地身份，保证 tabBar 和所有页面都能读到正确的身份 ──
    const cached = wx.getStorageSync('currentUser');

    // 判定缓存是否有效：必须有 role 且（role 是 teacher 时必须有 counselorId）
    const isValid = cached && cached.role &&
      (cached.role !== 'teacher' || cached.counselorId);

    if (!isValid) {
      // 缓存无效 → 清除并写入干净的开发用老师身份
      console.warn('⚠️ 检测到无效缓存（缺 counselorId 或 role），已重置');
      const devUser = {
        id: 1,
        openid: 'dev_teacher',
        nickname: '张老师',
        avatar: '',
        role: 'teacher',
        counselorId: 1
      };
      wx.setStorageSync('currentUser', devUser);
      console.log('🛠️ 已写入默认老师身份 =>', JSON.stringify(devUser));
    } else {
      console.log('📦 使用有效缓存身份 => role:', cached.role, 'counselorId:', cached.counselorId);
    }

    // ── 异步登录，成功则用后端返回的身份覆盖默认 ──
    wx.login({
      success: res => {
        if (res.code) {
          console.log('🔑 微信登录 code 获取成功');
          request({
            url: 'https://botany-refined-pleading.ngrok-free.dev/api/wx/login',
            method: 'POST',
            data: { code: res.code },
            success: (backendRes: any) => {
              console.log('🔑 后端登录返回 =>', JSON.stringify(backendRes.data).substring(0, 300));
              if (backendRes.statusCode === 200 && backendRes.data && backendRes.data.userInfo) {
                const info = backendRes.data.userInfo;
                // 确保后端返回的身份也包含 counselorId
                if (info.role === 'teacher' && !info.counselorId) {
                  console.warn('⚠️ 后端登录返回的教师身份缺少 counselorId，回退使用本地身份');
                  return;
                }
                wx.setStorageSync('currentUser', info);
                console.log('✅ 登录成功，角色：', info.role, 'counselorId:', info.counselorId);
              } else {
                console.warn('⚠️ 登录返回格式异常，继续使用本地身份');
              }
            },
            fail: (err: any) => {
              console.warn('⚠️ 后端登录接口不通，继续使用本地身份', err);
            }
          })
        }
      },
      fail: (err: any) => {
        console.warn('⚠️ wx.login 调用失败，继续使用本地身份', err);
      }
    })
  }
})
