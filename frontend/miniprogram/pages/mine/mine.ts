import { request } from '../../utils/util';

Page({
  data: {
    // 角色
    role: '' as string,
    userId: 0,
    currentDate: '',

    // 老师用
    teacherInfo: {
      id: 0,
      name: '',
      title: '',
      avatar: '',
      shortDesc: '',
      fullDesc: '',
      specialties: '',
      phone: ''
    },
    studentCount: 0,
    isEditing: false,
    editForm: {
      shortDesc: '',
      fullDesc: '',
      specialties: '',
      phone: ''
    },

    // 学生用
    studentInfo: {
      realName: '',
      gender: '',
      birthDate: '',
      age: 0
    },
    pickerBirthDate: '',  // picker 兼容格式 "YYYY-MM"
    isEditingStudent: false,
    studentEditForm: {
      realName: '',
      gender: '',
      birthDate: ''
    },

    // 绑定相关 — 老师
    bindCode: '',
    bindRequests: [] as any[],
    requestCount: 0,

    // 绑定相关 — 学生
    bindCodeInput: '',
    myTeacher: null as any,
    bindStatus: 'none'  // 'none' | 'pending' | 'bound'
  },

  onShow() {
    // 设置今天的日期作为日期选择器上限（年月格式）
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth() + 1).padStart(2, '0');
    this.setData({ currentDate: `${y}-${m}` });

    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 });
      this.getTabBar().buildTabList();
    }

    const userInfo = wx.getStorageSync('currentUser');
    const role = userInfo?.role || 'student';
    const userId = userInfo?.id || 0;
    this.setData({ role, userId });

    if (role === 'teacher') {
      this.fetchTeacherProfile();
      this.fetchStudentCount();
      this.fetchBindCode();
      this.fetchBindRequests();
    } else {
      this.fetchStudentProfile();
      this.fetchMyTeacher();
    }
  },

  // ===================== 老师逻辑（保持现有） =====================

  fetchTeacherProfile() {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';
    const userId = userInfo?.id || '';

    wx.showLoading({ title: '加载中...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/profile',
      data: { counselorId, teacherId: userId },
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data) {
          const info = this.extractProfile(res.data);
          if (info.name) {
            this.applyProfile(info);
            return;
          }
        }
        this.fallbackLoadCounselorList();
      },
      fail: () => {
        wx.hideLoading();
        this.fallbackLoadCounselorList();
      }
    });
  },

  fallbackLoadCounselorList() {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId;
    const nickname = userInfo?.nickname || userInfo?.name;

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/counselors',
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data) {
          let list: any[] = [];
          if (Array.isArray(res.data)) list = res.data;
          else if (Array.isArray(res.data.data)) list = res.data.data;
          else if (Array.isArray(res.data.list)) list = res.data.list;

          let matched = null;
          if (counselorId) matched = list.find((c: any) => c.id === counselorId);
          if (!matched && nickname) matched = list.find((c: any) => c.name === nickname);

          if (matched) {
            this.applyProfile(matched);
            return;
          }
        }
        this.loadFromCache();
      },
      fail: () => this.loadFromCache()
    });
  },

  loadFromCache() {
    const userInfo = wx.getStorageSync('currentUser');
    if (userInfo) {
      this.setData({
        teacherInfo: {
          id: userInfo.counselorId || userInfo.id || 0,
          name: userInfo.nickname || userInfo.name || '',
          title: userInfo.title || '',
          avatar: userInfo.avatar || '',
          shortDesc: '', fullDesc: '', specialties: '', phone: ''
        }
      });
      wx.showToast({ title: '请检查后端服务是否启动', icon: 'none' });
    }
  },

  extractProfile(data: any): any {
    if (data.name && !data.data && !data.profile) return data;
    return data.data || data.profile || data.info || data;
  },

  applyProfile(info: any) {
    this.setData({
      teacherInfo: {
        id: info.id || 0,
        name: info.name || '',
        title: info.title || '',
        avatar: info.avatar || '',
        shortDesc: info.shortDesc || info.short_desc || '',
        fullDesc: info.fullDesc || info.full_desc || '',
        specialties: info.specialties || '',
        phone: info.phone || ''
      },
      editForm: {
        shortDesc: info.shortDesc || info.short_desc || '',
        fullDesc: info.fullDesc || info.full_desc || '',
        specialties: info.specialties || '',
        phone: info.phone || ''
      }
    });
  },

  fetchStudentCount() {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/students',
      data: { counselorId },
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data) {
          const list = Array.isArray(res.data) ? res.data : (res.data.list || res.data.data || []);
          this.setData({ studentCount: list.length });
        }
      },
      fail: () => {}
    });
  },

  startEdit() {
    this.setData({ isEditing: true });
  },

  cancelEdit() {
    const { teacherInfo } = this.data;
    this.setData({
      isEditing: false,
      editForm: {
        shortDesc: teacherInfo.shortDesc || '',
        fullDesc: teacherInfo.fullDesc || '',
        specialties: teacherInfo.specialties || '',
        phone: teacherInfo.phone || ''
      }
    });
  },

  onFieldInput(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`editForm.${field}`]: e.detail.value });
  },

  saveProfile() {
    const { editForm } = this.data;

    wx.showLoading({ title: '保存中...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/teacher/profile',
      method: 'PUT',
      data: editForm,
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          wx.showToast({ title: '保存成功', icon: 'success' });
          const ti = this.data.teacherInfo;
          this.setData({
            isEditing: false,
            teacherInfo: { ...ti, shortDesc: editForm.shortDesc, fullDesc: editForm.fullDesc, specialties: editForm.specialties, phone: editForm.phone }
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
  },

  // ===================== 学生逻辑 =====================

  fetchStudentProfile() {
    wx.showLoading({ title: '加载中...' });

    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/user/profile?userId=${this.data.userId}`,
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data) {
          const d = res.data;
          // 格式化显示用日期（年-月）
          let displayDate = '';
          let pickerDate = '';
          if (d.birthDate) {
            const parts = d.birthDate.split('-'); // "YYYY-MM-DD" or "YYYY-MM"
            if (parts.length >= 2) {
              displayDate = `${parts[0]}年${parts[1]}月`;
              pickerDate = `${parts[0]}-${parts[1]}`;
            }
          }
          this.setData({
            studentInfo: {
              realName: d.realName || '',
              gender: d.gender || '',
              birthDate: displayDate,
              age: d.age || 0
            },
            pickerBirthDate: pickerDate,
            studentEditForm: {
              realName: d.realName || '',
              gender: d.gender || '',
              birthDate: pickerDate
            }
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  startEditStudent() {
    const { studentInfo, pickerBirthDate } = this.data;
    this.setData({
      isEditingStudent: true,
      studentEditForm: {
        realName: studentInfo.realName || '',
        gender: studentInfo.gender || '',
        birthDate: pickerBirthDate || ''
      }
    });
  },

  cancelEditStudent() {
    this.setData({ isEditingStudent: false });
  },

  onStudentFieldInput(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`studentEditForm.${field}`]: e.detail.value });
  },

  onGenderChange(e: any) {
    const genders = ['男', '女', '其他'];
    const index = parseInt(e.detail.value);
    this.setData({ 'studentEditForm.gender': genders[index] || '' });
  },

  onDateChange(e: any) {
    this.setData({ 'studentEditForm.birthDate': e.detail.value });
  },

  saveStudentProfile() {
    const { userId, studentEditForm } = this.data;

    // birthDate 是 "YYYY-MM" 格式，补齐为 "YYYY-MM-01" 以兼容 MySQL DATE 类型
    const birthDate = studentEditForm.birthDate
      ? studentEditForm.birthDate + '-01'
      : '';

    wx.showLoading({ title: '保存中...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/user/profile',
      method: 'PUT',
      data: {
        userId,
        realName: studentEditForm.realName,
        gender: studentEditForm.gender,
        birthDate
      },
      success: (res: any) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '保存成功', icon: 'success' });
          this.setData({ isEditingStudent: false });
          this.fetchStudentProfile();
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  },

  // ===================== 绑定 — 老师端 =====================

  fetchBindCode() {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';
    if (!counselorId) return;

    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/teacher/bind-code?counselorId=${counselorId}`,
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data && res.data.bindCode) {
          this.setData({ bindCode: res.data.bindCode });
        }
      },
      fail: () => {}
    });
  },

  fetchBindRequests() {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';
    if (!counselorId) return;

    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/teacher/bind-requests?counselorId=${counselorId}`,
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data) {
          const list = res.data.requests || [];
          this.setData({
            bindRequests: list,
            requestCount: list.length
          });
        }
      },
      fail: () => {}
    });
  },

  approveRequest(e: any) {
    const requestId = e.currentTarget.dataset.id;
    this.handleRequest(requestId, 'approved', '已通过绑定');
  },

  rejectRequest(e: any) {
    const requestId = e.currentTarget.dataset.id;
    this.handleRequest(requestId, 'rejected', '已拒绝申请');
  },

  handleRequest(requestId: number, status: string, msg: string) {
    wx.showLoading({ title: '处理中...' });
    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/teacher/bind-requests/${requestId}`,
      method: 'PUT',
      data: { status },
      success: (res: any) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: msg, icon: 'success' });
          this.fetchBindRequests();
          this.fetchStudentCount();
        } else {
          wx.showToast({ title: '操作失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  },

  // ===================== 绑定 — 学生端 =====================

  fetchMyTeacher() {
    request({
      url: `https://botany-refined-pleading.ngrok-free.dev/api/student/my-teacher?studentId=${this.data.userId}`,
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data) {
          if (res.data.bound) {
            this.setData({
              bindStatus: 'bound',
              myTeacher: {
                name: res.data.name || '',
                title: res.data.title || '',
                avatar: res.data.avatar || ''
              }
            });
          } else if (res.data.pending) {
            this.setData({ bindStatus: 'pending' });
          } else {
            this.setData({ bindStatus: 'none' });
          }
        }
      },
      fail: () => {}
    });
  },

  onBindCodeInput(e: any) {
    this.setData({ bindCodeInput: e.detail.value });
  },

  submitBindRequest() {
    const { userId, bindCodeInput } = this.data;
    const code = bindCodeInput.trim();

    if (!code) {
      wx.showToast({ title: '请输入4位绑定码', icon: 'none' });
      return;
    }
    if (code.length !== 4 || !/^\d{4}$/.test(code)) {
      wx.showToast({ title: '绑定码应为4位数字', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '提交中...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/student/bind-request',
      method: 'POST',
      data: { studentId: userId, bindCode: code },
      success: (res: any) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '申请已发送', icon: 'success' });
          this.setData({ bindStatus: 'pending', bindCodeInput: '' });
        } else {
          wx.showToast({ title: res.data?.error || '提交失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  },

  // ===================== 通用 =====================

  goToMyStudents() {
    wx.navigateTo({
      url: '/pages/studentList/studentList'
    });
  },

  onChangeAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0];
        this.setData({ 'teacherInfo.avatar': tempFilePath });
        wx.showToast({ title: '头像上传需接入云存储', icon: 'none' });
      }
    });
  },

  // 🔧 开发者账号切换（长按头像触发）
  onDevSwitch() {
    wx.showActionSheet({
      itemList: [
        '🔴 张老师 (counselorId=1)',
        '🔵 小明 — 已绑定 (student)',
        '🟢 未绑定学生 (student)',
        '取消'
      ],
      success: (res) => {
        let user: any;
        switch (res.tapIndex) {
          case 0:
            user = { id: 1, openid: 'dev_teacher', nickname: '张老师', avatar: '', role: 'teacher', counselorId: 1 };
            break;
          case 1:
            user = { id: 2, openid: 'dev_student_xm', nickname: '小明', avatar: '', role: 'student' };
            break;
          case 2:
            user = { id: 5, openid: 'dev_student_free', nickname: '未绑定学生', avatar: '', role: 'student' };
            break;
          default:
            return;
        }
        wx.setStorageSync('currentUser', user);
        wx.showToast({ title: '已切换，重启生效', icon: 'none', duration: 2000 });
        setTimeout(() => {
          wx.reLaunch({ url: '/pages/home/home' });
        }, 1500);
      }
    });
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      success: (res) => {
        if (res.confirm) {
          wx.clearStorageSync();
          wx.reLaunch({ url: '/pages/home/home' });
        }
      }
    });
  }
})
