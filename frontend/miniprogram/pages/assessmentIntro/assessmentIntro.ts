import { request } from '../../utils/util';

Page({
  data: {
    assessmentId: null,
    title: "",
    description: ""
  },

  onLoad(options: any) {
    const id = options.id;
    this.setData({ assessmentId: id });

    // 向后端请求量表的详情数据
    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/assessment?id=' + id,
      method: 'GET',
      success: (res: any) => {
        this.setData({
          title: res.data.title,
          description: res.data.description
        });
      }
    })
  },

  // 点击按钮，真正进入考场
  startRealTest() {
    wx.navigateTo({
      url: `/pages/assessmentDo/assessmentDo?id=${this.data.assessmentId}`
    });
  }
})