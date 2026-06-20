import { request } from '../../utils/util';

Page({
  data: {
    counselors: [] as any[]
  },

  onLoad() {
    this.fetchCounselors();
  },

  onShow() {
    this.fetchCounselors();
  },

  fetchCounselors() {
    const backendUrl = 'https://botany-refined-pleading.ngrok-free.dev/api/counselors';

    wx.showLoading({ title: '加载中...' });

    request({
      url: backendUrl,
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        console.log('📡 咨询师API返回 =>', JSON.stringify(res.data).substring(0, 500));

        if (res.statusCode === 200 && res.data) {
          let list: any[] = [];

          // 兼容多种后端返回格式
          if (Array.isArray(res.data)) {
            // 格式1: 直接返回数组 [{...}, {...}]
            list = res.data;
          } else if (Array.isArray(res.data.data)) {
            // 格式2: 包装在 data 字段 { code:200, data: [...] }
            list = res.data.data;
          } else if (Array.isArray(res.data.list)) {
            // 格式3: 包装在 list 字段 { list: [...] }
            list = res.data.list;
          } else if (Array.isArray(res.data.records)) {
            // 格式4: 包装在 records 字段
            list = res.data.records;
          } else if (Array.isArray(res.data.counselors)) {
            // 格式5: 包装在 counselors 字段
            list = res.data.counselors;
          } else {
            console.warn('⚠️ 未知的返回格式，原始数据:', res.data);
          }

          if (list.length > 0) {
            this.setData({ counselors: list });
            console.log(`✅ 成功加载 ${list.length} 位咨询师`);
          } else {
            console.warn('⚠️ 解析后的咨询师列表为空');
            wx.showToast({ title: '暂无咨询师数据', icon: 'none' });
          }
        } else if (res.statusCode === 200) {
          // statusCode 200 但 data 为空
          console.warn('⚠️ API 返回 200 但 data 为空');
          wx.showToast({ title: '暂无咨询师数据', icon: 'none' });
        }
      },
      fail: (err: any) => {
        wx.hideLoading();
        console.error('❌ 获取咨询师列表失败', err);
        console.error('请求URL:', backendUrl);
        wx.showToast({ title: '网络连接失败，请确认后端已启动', icon: 'none' });
      }
    });
  },

  goToDetail(e: any) {
    const counselorId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/counselorDetail/counselorDetail?id=${counselorId}`
    });
  }
})
