import { request } from '../../utils/util';

Page({
  data: {
    students: [] as any[],
    searchKeyword: ''
  },

  onShow() {
    this.fetchStudents();
  },

  // 获取我的学生列表
  fetchStudents() {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';

    console.log('🔍 我的学生 — 当前身份 =>', { counselorId, nickname: userInfo?.nickname, role: userInfo?.role });

    wx.showLoading({ title: '加载中...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/students',
      data: { counselorId },
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        console.log('🔍 我的学生 — 后端返回 => status:', res.statusCode, 'data keys:', Object.keys(res.data || {}));

        if (res.statusCode === 200 && res.data) {
          // 兼容多种返回格式
          let list: any[] = [];
          if (Array.isArray(res.data)) {
            list = res.data;
          } else if (Array.isArray(res.data.students)) {
            list = res.data.students;
          } else if (Array.isArray(res.data.list)) {
            list = res.data.list;
          } else if (Array.isArray(res.data.data)) {
            list = res.data.data;
          } else if (res.data.error) {
            console.warn('🔍 后端返回错误：', res.data.error);
          }

          console.log('🔍 解析到', list.length, '个学生');
          this.setData({ students: list });
        } else if (res.data && res.data.error) {
          console.warn('🔍 后端返回错误：', res.data.error);
          wx.showToast({ title: res.data.error, icon: 'none' });
        }
      },
      fail: (err: any) => {
        wx.hideLoading();
        console.error('获取学生列表失败', err);
      }
    });
  },

  // 搜索输入
  onSearchInput(e: any) {
    this.setData({ searchKeyword: e.detail.value });
  },

  // 执行搜索
  onSearch() {
    const keyword = this.data.searchKeyword.trim();
    if (!keyword) {
      this.fetchStudents();
      return;
    }

    // 前端直接过滤，不请求后端
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';

    wx.showLoading({ title: '搜索中...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/students',
      data: { counselorId, keyword },
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data) {
          let list: any[] = [];
          if (Array.isArray(res.data)) list = res.data;
          else if (Array.isArray(res.data.students)) list = res.data.students;
          else if (Array.isArray(res.data.list)) list = res.data.list;
          this.setData({ students: list });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '搜索失败', icon: 'none' });
      }
    });
  },

  // 点击学生 → 查看该学生的测评记录
  onStudentTap(e: any) {
    const studentId = e.currentTarget.dataset.id;
    const studentName = e.currentTarget.dataset.name;

    if (!studentId) {
      wx.showToast({ title: '缺少学生ID', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '加载测评记录...' });

    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/teacher/students/${studentId}/records`,
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data) {
          wx.setStorageSync('studentRecords', {
            studentName,
            records: Array.isArray(res.data) ? res.data : (res.data.records || [])
          });
          const studentAvatar = e.currentTarget.dataset.avatar || '';
          wx.navigateTo({
            url: `/pages/teacherDashboard/teacherDashboard?studentId=${studentId}&studentName=${encodeURIComponent(studentName)}&studentAvatar=${encodeURIComponent(studentAvatar)}`
          });
        } else {
          wx.showToast({ title: '暂无测评记录', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  }
});
