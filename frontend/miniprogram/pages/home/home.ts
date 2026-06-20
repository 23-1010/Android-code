Page({
  data: {
    userRole: 'student'
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
      this.getTabBar().buildTabList();
    }

    const userInfo = wx.getStorageSync('currentUser');
    if (userInfo && userInfo.role) {
      this.setData({ userRole: userInfo.role });
    }
  },

  goToCounselors() {
    wx.navigateTo({ url: '/pages/index/index' });
  },

  goToAssessments() {
    wx.navigateTo({ url: '/pages/assessment/assessment' });
  },

})
