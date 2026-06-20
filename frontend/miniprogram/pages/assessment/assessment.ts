import { request } from '../../utils/util';

Page({
  data: {
    assessmentList: [] as any[]// 准备一个空数组，用来接收后端传来的量表数据
  },

  onLoad() {
    // 页面一加载，就去请求后端数据
    this.getAssessmentData();
  },

  getAssessmentData() {
    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/assessments',
      method: 'GET',
      success: (res:any) => {
        console.log("从后端拿到的量表数据：", res.data);
        // 把数据存入 data 中供页面渲染
        this.setData({
          assessmentList: res.data
        });
      },
      fail: (err) => {
        console.error("请求失败", err);
      }
    })
  },
  goToTest(e: any) {
    // 获取刚才绑在按钮上的量表 ID
    const assessmentId = e.currentTarget.dataset.id;
    console.log("准备前往答题页面，量表ID为：", assessmentId);

    // 使用 wx.navigateTo 进行页面跳转，并把 ID 作为参数带在网址后面
    wx.navigateTo({
      url: `/pages/assessmentIntro/assessmentIntro?id=${assessmentId}`
    });
  }
})