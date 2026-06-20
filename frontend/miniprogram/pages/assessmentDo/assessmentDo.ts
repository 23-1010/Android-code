import { request } from '../../utils/util';

Page({
  data: {
    assessmentId: null,
    paperTitle: "",
    questions: [],
    userAnswers: {}
  },

  onLoad(options: any) {
    const id = options.id;
    this.setData({ assessmentId: id });
    this.getPaperData(id);
  },

  getPaperData(id: string) {
    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/assessment?id=' + id, 
      method: 'GET',
      success: (res: any) => {
        const paperJson = JSON.parse(res.data.questionsJson);
        this.setData({
          paperTitle: res.data.title,
          questions: paperJson.questions 
        });
      }
    })
  },

  // 当用户点击任意一个选项时触发
  onRadioChange(e: any) {
    const questionId = e.currentTarget.dataset.qid; 
    const score = e.detail.value;                   
    
    let currentAnswers = this.data.userAnswers as Record<string, number>;
    currentAnswers[questionId] = parseInt(score); 

    this.setData({
      userAnswers: currentAnswers
    });

    console.log("当前答题卡状态：", this.data.userAnswers);
  },

  // 当用户点击“提交答卷”按钮时触发
  submitTest() {
    const answers = this.data.userAnswers;

    if (Object.keys(answers).length === 0) {
      wx.showToast({ title: '您还没有答题哦', icon: 'none' });
      return;
    }

    // 👇 【新增步骤 1】：从本地缓存中获取当前静默登录用户的身份信息
    const userInfo = wx.getStorageSync('currentUser');
    const currentUserId = userInfo ? userInfo.id : null;

    wx.showLoading({ title: '正在计算得分并存档...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/calculate',
      method: 'POST',
      data: {
        assessmentId: this.data.assessmentId, 
        answers: answers,
        // 👇 【新增步骤 2】：把用户的身份凭证（userId）发给后端入库
        userId: currentUserId 
      },
      success: (res) => {
        wx.hideLoading();
        console.log("批卷与存档完成！", res.data);
        
        // 把后端算好的分数存到手机本地缓存中
        wx.setStorageSync('currentResult', res.data);
        
        // 带着成绩单，直接重定向（跳转）到结果页
        wx.redirectTo({
          url: '/pages/assessmentResult/assessmentResult'
        });
      },
      fail: (err) => {
        wx.hideLoading();
        console.error("交卷失败", err);
      }
    })
  }
})