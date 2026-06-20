import { request } from '../../utils/util';

Page({
  data: {
    records: [] as any[],
    studentId: '',
    studentName: '',
    studentAvatar: '',
    // 学生个人资料
    studentProfile: {
      realName: '',
      gender: '',
      birthDate: '',
      age: 0
    },
    // 老师备注
    teacherNotes: '',
    isEditingNotes: false,
    editingNotes: '',
    // 当前老师ID
    teacherId: 0
  },

  onLoad(options: any) {
    const userInfo = wx.getStorageSync('currentUser');
    const teacherId = userInfo?.id || 0;

    if (options.studentId) {
      this.setData({
        studentId: options.studentId,
        studentName: decodeURIComponent(options.studentName || ''),
        studentAvatar: decodeURIComponent(options.studentAvatar || ''),
        teacherId
      });
      wx.setNavigationBarTitle({
        title: this.data.studentName ? `${this.data.studentName} 的测评记录` : '学生测评记录'
      });
    }
  },

  onShow() {
    if (this.data.studentId) {
      this.fetchStudentRecords(this.data.studentId);
    } else {
      this.fetchStudentRecords();
    }
  },

  fetchStudentRecords(studentId?: string) {
    let url = 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/records';
    if (studentId) {
      url = `https://botany-refined-pleading.ngrok-free.dev/api/teacher/students/${studentId}/records?teacherId=${this.data.teacherId}`;
    }

    request({
      url,
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200) {
          const data = Array.isArray(res.data) ? res.data : (res.data.records || res.data || []);
          this.setData({ records: data });

          // 提取学生个人资料
          if (res.data.profile) {
            const p = res.data.profile;
            this.setData({
              studentProfile: {
                realName: p.realName || '',
                gender: p.gender || '',
                birthDate: p.birthDate || '',
                age: p.age || 0
              },
              teacherNotes: res.data.teacherNotes || '',
              editingNotes: res.data.teacherNotes || ''
            });
            // 如果有真实姓名，更新标题
            if (p.realName) {
              wx.setNavigationBarTitle({
                title: `${p.realName} 的测评记录`
              });
            }
          }
        }
      },
      fail: (err: any) => {
        console.error('获取测评记录失败', err);
      }
    });
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

  // ===== 备注编辑 =====
  startEditNotes() {
    this.setData({
      isEditingNotes: true,
      editingNotes: this.data.teacherNotes
    });
  },

  cancelEditNotes() {
    this.setData({
      isEditingNotes: false,
      editingNotes: this.data.teacherNotes
    });
  },

  onNotesInput(e: any) {
    this.setData({ editingNotes: e.detail.value });
  },

  saveNotes() {
    const { studentId, teacherId, editingNotes } = this.data;

    wx.showLoading({ title: '保存中...' });

    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/teacher/students/${studentId}/notes`,
      method: 'PUT',
      data: { teacherId, notes: editingNotes },
      success: (res: any) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '备注已保存', icon: 'success' });
          this.setData({
            isEditingNotes: false,
            teacherNotes: editingNotes
          });
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  }
})
