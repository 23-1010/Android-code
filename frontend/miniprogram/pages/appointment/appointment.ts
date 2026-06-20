import { request } from '../../utils/util';

interface DayInfo {
  day: number;
  dateStr: string;
  hasAppointment: boolean;
  isToday: boolean;
  isOtherMonth: boolean;
}

Page({
  data: {
    // 当前显示的年份和月份
    currentYear: 2026,
    currentMonth: 6,
    // 星期标题
    weekdays: ['日', '一', '二', '三', '四', '五', '六'],
    // 日历日期网格
    days: [] as DayInfo[],
    // 标记有预约的日期集合
    markedDates: {} as Record<string, boolean>,
    // 当前选中的日期
    selectedDate: '',
    // 选中日期的预约列表
    appointments: [] as any[],
    // 是否展示详情面板
    showDetail: false,
    // 当月预约总数
    monthAppointmentCount: 0
  },

  onShow() {
    // 更新底部 tab 选中态
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
      this.getTabBar().buildTabList();
    }

    const now = new Date();
    this.setData({
      currentYear: now.getFullYear(),
      currentMonth: now.getMonth() + 1
    });
    this.renderCalendar();
    this.fetchMonthAppointments();
  },

  // 渲染日历
  renderCalendar() {
    const { currentYear, currentMonth, markedDates } = this.data;
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    // 当月第一天
    const firstDay = new Date(currentYear, currentMonth - 1, 1);
    // 当月最后一天
    const lastDay = new Date(currentYear, currentMonth, 0);
    const daysInMonth = lastDay.getDate();
    // 第一天是周几（0=周日）
    const startWeekDay = firstDay.getDay();

    const days: DayInfo[] = [];

    // 上月填充
    const prevMonthLastDay = new Date(currentYear, currentMonth - 1, 0).getDate();
    for (let i = startWeekDay - 1; i >= 0; i--) {
      const d = prevMonthLastDay - i;
      const dateStr = this.buildDateStr(currentYear, currentMonth - 1, d);
      days.push({
        day: d,
        dateStr,
        hasAppointment: !!markedDates[dateStr],
        isToday: dateStr === todayStr,
        isOtherMonth: true
      });
    }

    // 当月日期
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = this.buildDateStr(currentYear, currentMonth, d);
      days.push({
        day: d,
        dateStr,
        hasAppointment: !!markedDates[dateStr],
        isToday: dateStr === todayStr,
        isOtherMonth: false
      });
    }

    // 下月填充（补齐到 6 行 x 7 列）
    const remaining = 42 - days.length;
    for (let d = 1; d <= remaining; d++) {
      const dateStr = this.buildDateStr(currentYear, currentMonth + 1, d);
      days.push({
        day: d,
        dateStr,
        hasAppointment: !!markedDates[dateStr],
        isToday: dateStr === todayStr,
        isOtherMonth: true
      });
    }

    this.setData({ days });
  },

  buildDateStr(year: number, month: number, day: number): string {
    // 处理月份溢出
    let m = month;
    let y = year;
    if (m <= 0) { m = 12; y--; }
    if (m > 12) { m = 1; y++; }
    return `${y}-${String(m).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  },

  // 获取当月所有预约（用于日历打点）
  fetchMonthAppointments() {
    const { currentYear, currentMonth } = this.data;
    const monthStr = `${currentYear}-${String(currentMonth).padStart(2, '0')}`;

    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/appointments',
      data: { month: monthStr, counselorId },
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200 && res.data) {
          // 后端返回 { month, dailyCounts: [{date, count}, ...] }
          const dailyCounts = res.data.dailyCounts || res.data.dates || [];
          let markedDates: Record<string, boolean> = {};
          let totalCount = 0;

          if (Array.isArray(dailyCounts)) {
            dailyCounts.forEach((item: any) => {
              markedDates[item.date] = true;
              totalCount += (item.count || 1);
            });
          }

          this.setData({ markedDates, monthAppointmentCount: totalCount });
          this.renderCalendar();
        }
      },
      fail: (err: any) => {
        console.error('获取当月预约失败', err);
      }
    });
  },

  // 点击日期
  onDayTap(e: any) {
    const dateStr = e.currentTarget.dataset.date;
    const isOtherMonth = e.currentTarget.dataset.otherMonth;

    // 点击其他月份的日期 → 切换月份
    if (isOtherMonth) {
      const parts = dateStr.split('-');
      this.setData({
        currentYear: parseInt(parts[0]),
        currentMonth: parseInt(parts[1])
      });
      this.renderCalendar();
      this.fetchMonthAppointments();
      return;
    }

    this.setData({
      selectedDate: dateStr,
      showDetail: true
    });
    this.fetchDayAppointments(dateStr);
  },

  // 获取某天的预约详情
  fetchDayAppointments(dateStr: string) {
    const userInfo = wx.getStorageSync('currentUser');
    const counselorId = userInfo?.counselorId || '';

    // 先清空旧数据，展示加载中状态
    this.setData({ appointments: [] });
    wx.showLoading({ title: '加载预约...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/appointments',
      data: { date: dateStr, counselorId },
      method: 'GET',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          const list = Array.isArray(res.data) ? res.data : (res.data.appointments || []);
          this.setData({ appointments: list });
        } else {
          // 后端返回非 200（如 400/500），清空列表展示"暂无预约"
          console.warn('当日预约接口返回', res.statusCode);
          this.setData({ appointments: [] });
        }
      },
      fail: (err: any) => {
        wx.hideLoading();
        console.error('获取当日预约失败', err);
        // 失败也清空列表，让空状态自然展示
        this.setData({ appointments: [] });
      }
    });
  },

  // 修改预约状态
  onStatusChange(e: any) {
    const appointmentId = e.currentTarget.dataset.id;
    const newStatus = e.currentTarget.dataset.status;

    wx.showModal({
      title: '确认操作',
      content: `确定要将此预约标记为「${this.statusLabel(newStatus)}」吗？`,
      success: (modalRes) => {
        if (!modalRes.confirm) return;

        request({
          url: `https://botany-refined-pleading.ngrok-free.dev/api/appointments/${appointmentId}`,
          method: 'PUT',
          data: { status: newStatus },
          success: (res: any) => {
            if (res.statusCode === 200) {
              wx.showToast({ title: '更新成功', icon: 'success' });
              // 刷新当日列表和月度标记
              this.fetchDayAppointments(this.data.selectedDate);
              this.fetchMonthAppointments();
            } else {
              wx.showToast({ title: '更新失败', icon: 'none' });
            }
          },
          fail: () => {
            wx.showToast({ title: '网络请求失败', icon: 'none' });
          }
        });
      }
    });
  },

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      pending: '待确认',
      confirmed: '已确认',
      cancelled: '已取消',
      completed: '已完成'
    };
    return map[status] || status;
  },

  // 关闭详情面板
  closeDetail() {
    this.setData({ showDetail: false, appointments: [] });
  },

  // 上个月
  prevMonth() {
    let { currentYear, currentMonth } = this.data;
    if (currentMonth === 1) {
      currentYear -= 1;
      currentMonth = 12;
    } else {
      currentMonth -= 1;
    }
    // 先算好再 setData，回调里用新值请求数据
    this.setData({ currentYear, currentMonth, showDetail: false }, () => {
      this.renderCalendar();
      this.fetchMonthAppointments();
    });
  },

  // 下个月
  nextMonth() {
    let { currentYear, currentMonth } = this.data;
    if (currentMonth === 12) {
      currentYear += 1;
      currentMonth = 1;
    } else {
      currentMonth += 1;
    }
    this.setData({ currentYear, currentMonth, showDetail: false }, () => {
      this.renderCalendar();
      this.fetchMonthAppointments();
    });
  }
});
