import { request } from '../../utils/util';

Page({
  data: {
    stats: {
      totalStudents: 0,
      monthAssessments: 0,
      warningCount: 0,
      pendingAppointments: 0
    },
    // 后端返回的全部记录（未筛选）
    allRecords: [] as any[],
    // 当前页面展示的记录（已筛选）
    records: [] as any[],
    filterType: 'all'
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
      this.getTabBar().buildTabList();
    }

    this.fetchDashboardStats();
    this.fetchRecords();
  },

  fetchDashboardStats() {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/dashboard/stats',
      data: { counselorId },
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data) {
          const d = res.data;
          this.setData({
            stats: {
              totalStudents: d.totalStudents || d.studentCount || 0,
              monthAssessments: d.monthAssessments || d.monthAssessmentCount || 0,
              warningCount: d.warningCount || 0,
              pendingAppointments: d.pendingAppointments || d.pendingAppointmentCount || 0
            }
          });
        }
      },
      fail: (err: any) => {
        console.error('获取统计数据失败', err);
      }
    });
  },

  fetchRecords() {
    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/records',
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data) {
          const list = Array.isArray(res.data) ? res.data : [];
          this.setData({ allRecords: list });
          this.applyFilter();
        }
      },
      fail: (err: any) => {
        console.error('获取记录失败', err);
      }
    });
  },

  // 筛选切换
  onFilterTap(e: any) {
    const type = e.currentTarget.dataset.type;
    this.setData({ filterType: type }, () => {
      this.applyFilter();
    });
  },

  // 根据 filterType 过滤 allRecords → records
  applyFilter() {
    const { allRecords, filterType } = this.data;
    let filtered: any[];
    if (filterType === 'all') {
      filtered = allRecords;
    } else {
      // filterType 就是 scale_type 值，如 'SCL90', 'SDS'
      filtered = allRecords.filter((r: any) => r.scale_type === filterType);
    }
    this.setData({ records: filtered });
  },

  onRecordTap(e: any) {
    const recordId = e.currentTarget.dataset.recordId;
    if (!recordId) {
      wx.showToast({ title: '缺少记录ID', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '加载详情...' });

    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/teacher/records/${recordId}`,
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data) {
          wx.setStorageSync('currentResult', res.data);
          wx.navigateTo({
            url: '/pages/assessmentResult/assessmentResult'
          });
        } else {
          wx.showToast({ title: '暂无详情数据', icon: 'none' });
        }
      },
      fail: (err: any) => {
        wx.hideLoading();
        console.error('获取记录详情失败', err);
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  },

  onPullDownRefresh() {
    this.fetchDashboardStats();
    this.fetchRecords();
    wx.stopPullDownRefresh();
  }
});
