Component({
  data: {
    selected: 0,
    list: [] as { pagePath: string; text: string; dot?: boolean }[]
  },

  lifetimes: {
    attached() {
      this.buildTabList();
      // 🔧 延迟重查：app.onLaunch 里的 wx.login 是异步的，
      // 回调可能在 tabBar 首次渲染之后才把身份写入 storage，
      // 所以 800ms 后再读一次确保拿到正确的 role
      setTimeout(() => {
        this.buildTabList();
      }, 800);
    }
  },

  pageLifetimes: {
    show() {
      this.buildTabList();
    }
  },

  methods: {
    buildTabList() {
      const userInfo = wx.getStorageSync('currentUser');
      const role = userInfo?.role || '';
      const counselorId = userInfo?.counselorId;

      console.log('🔍 tabBar 读取身份 =>', { role, counselorId });

      // 老师身份判定：role 是 teacher，或者有关联的 counselorId
      const isTeacher = role === 'teacher' || !!counselorId;

      const allTabs = [
        { pagePath: '/pages/home/home',               text: '主页' },
        { pagePath: '/pages/backend/backend',          text: '后台' },
        { pagePath: '/pages/appointment/appointment',  text: '预约' },
        { pagePath: '/pages/mine/mine',                text: '我的' }
      ];

      const list = isTeacher ? allTabs : [allTabs[0], allTabs[3]];

      // 只在列表真正变化时才 setData，避免无意义的重渲染
      const currentStr = JSON.stringify(this.data.list.map((t: any) => t.text));
      const newStr = JSON.stringify(list.map(t => t.text));
      if (currentStr !== newStr) {
        console.log('🔍 tabBar 更新 =>', list.map(t => t.text).join(' | '));
        this.setData({ list });
      }
    },

    switchTab(e: WechatMiniprogram.TouchEvent) {
      const index = e.currentTarget.dataset.index;
      const item = this.data.list[index];
      if (!item) return;

      wx.switchTab({
        url: item.pagePath
      });
    }
  }
});
