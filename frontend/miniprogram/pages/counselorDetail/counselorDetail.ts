import { request } from '../../utils/util';

Page({
  data: {
    info: {} as any,
    specialtyList: [] as string[]
  },

  onLoad(options: any) {
    const counselorId = options.id;
    if (counselorId) {
      this.fetchDetail(counselorId);
    }
  },

  fetchDetail(id: number) {
    wx.showLoading({ title: '加载中...' });

    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/counselors/${id}`,
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data) {
          // 兼容多种返回格式
          let raw = res.data;
          // 如果嵌套在 data 字段里
          if (raw.data && !raw.name) raw = raw.data;

          // 字段映射
          if (!raw.detailContent) {
            raw.detailContent = raw.fullDesc || raw.full_desc || raw.detail_content || '';
          }
          if (!raw.shortDesc) {
            raw.shortDesc = raw.short_desc || raw.shortDesc || '';
          }
          if (!raw.specialties) {
            raw.specialties = raw.specialties || '';
          }

          // 预拆分为数组，wxml 不支持 .split()
          const specialtyList = raw.specialties
            ? raw.specialties.split(',').map((s: string) => s.trim()).filter(Boolean)
            : [];

          this.setData({ info: raw, specialtyList });
        }
      },
      fail: (err: any) => {
        wx.hideLoading();
        console.error('获取咨询师详情失败', err);
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  bookConsultation() {
    wx.showModal({
      title: '预约提示',
      content: '是否确认预约该咨询师？后续老师将通过系统联系您。',
      success(res) {
        if (res.confirm) {
          wx.showToast({ title: '预约申请已提交', icon: 'success' });
        }
      }
    });
  }
})
