// 引入 echarts 核心文件
const echarts = require('../../components/ec-canvas/echarts');
import { request } from '../../utils/util';

// 💡 核心武器：9大因子 × 4级评级的智能建议文案库
const adviceDict: Record<string, Record<string, string>> = {
  "躯体化": {
    "正常": "您的身体状况感觉良好，没有被莫名的躯体不适感所困扰，请继续保持健康的生活作息。",
    "轻度": "您偶尔会感到一些身体不适（如头痛、酸痛或肠胃不适），这可能是近期劳累或心理压力的身体反应。建议规律作息，适当运动和放松。",
    "中度": "身体的不适感已经较频繁地出现。建议您在前往综合医院排除器质性疾病后，关注自身情绪状态，适当寻求心理疏导。",
    "重度": "您正承受着强烈的身体不适感，严重影响日常生活。强烈建议您在排查身体疾病的同时，同步寻求专业心理科室的帮助。"
  },
  "强迫症状": {
    "正常": "您的思维和行为自由流畅，没有被无意义的想法或重复动作所困扰。",
    "轻度": "您偶尔会出现一些挥之不去的想法或需要重复核对的动作，但尚能自我控制。建议接纳这些偶尔的“小固执”，顺其自然。",
    "中度": "明知不必要却难以控制的想法或行为（如反复洗手、检查）开始增多，给您带来了一定苦恼。建议尝试注意力转移法，或寻求专业人士协助。",
    "重度": "强迫思维或行为已经严重干扰了您的工作和生活，让您感到十分痛苦和疲惫。建议尽快向专业的心理咨询师或精神科医生寻求系统治疗。"
  },
  "人际关系敏感": {
    "正常": "您在人际交往中感到自信和舒适，能够客观自如地与他人相处。",
    "轻度": "您在与人交往时偶尔会感到些许不自在或过于在意他人的负面评价。建议多给自己积极暗示，相信自身的内在价值。",
    "中度": "您常感到自卑、局促不安，并在交往中显得敏感多疑。建议尝试以开放的心态面对他人，循序渐进地参与社交，必要时可寻求心理辅导。",
    "重度": "强烈的自卑感和社交退缩已使您极度回避人际交往，严重影响了正常的社会功能。建议尽快通过专业心理治疗，重塑健康社交模式。"
  },
  "抑郁": {
    "正常": "您的抑郁程度较弱，生活态度乐观积极，充满活力，心境愉快。请继续保持这份阳光的心态！",
    "轻度": "您有时会感到生活缺乏趣味或动力不足，这可能是生活压力导致的暂时性低落。建议多做自己喜欢的事，多与亲友交流倾诉。",
    "中度": "情绪低落、悲观和精力减退的感觉较明显，可能已影响到您的日常生活。建议您高度重视，积极寻找减压途径，并寻求心理咨询师的疏导。",
    "重度": "您正承受着极度强烈的悲观与绝望情绪。生命非常宝贵，请务必立刻向最信任的亲友求助，并尽快前往精神专科医院接受专业治疗。"
  },
  "焦虑": {
    "正常": "您不易焦虑，内心保持着良好的安定状态。",
    "轻度": "您偶尔会感到神经过敏、内心紧张或烦躁，这是面对生活挑战的正常反应。建议尝试深呼吸、冥想等放松训练，舒缓紧绷的神经。",
    "中度": "莫名的游离性焦虑、惊恐或紧张感较常出现，已影响到心境安宁。建议学习情绪管理技巧，必要时寻求心理咨询的专业支持。",
    "重度": "强烈的焦虑、惊恐体验让您痛苦不堪，可能伴有心悸、颤抖等明显躯体反应。建议您尽快就医，获取精神科医生的专业诊断和有效干预。"
  },
  "敌对": {
    "正常": "您的脾气温和，待人友好，情绪控制与调节能力良好。",
    "轻度": "您有时会产生一些厌烦或争论的冲动，但基本能自我克制。建议在感到不满时，先深呼吸冷却情绪，尝试用温和的方式表达需求。",
    "中度": "容易发脾气、好争论或不可抑制的冲动较明显，可能已对人际关系造成影响。建议您学习愤怒管理技巧，寻找如运动等健康的宣泄途径。",
    "重度": "敌对思想、强烈的愤怒甚至破坏性行为的冲动非常强烈，难以自控。为了您和他人的安全与福祉，强烈建议尽快寻求专业的精神心理干预。"
  },
  "恐怖": {
    "正常": "您对日常的人、物或场景没有异常的恐惧感，内心安全感良好。",
    "轻度": "您对某些特定事物或场景（如出门、人多场合）有轻微的害怕，但不影响整体生活。建议理性看待恐惧源，适度面对，顺其自然。",
    "中度": "对特定对象或情境的恐惧感较明显，并导致您开始出现刻意回避的行为。建议尝试系统脱敏等心理调适方法，或寻求专业辅导。",
    "重度": "强烈的、不合理的恐惧感导致您极度回避某些日常场景，严重限制了个人生活自由。建议尽快寻求精神心理专科治疗。"
  },
  "偏执": {
    "正常": "您思维客观，人际信任度高，没有明显的猜疑心理。",
    "轻度": "您偶尔会对他人意图产生猜疑或感到委屈，这多是敏感或缺乏沟通所致。建议遇到疑问时多进行客观核实，避免主观臆断。",
    "中度": "猜疑心较重，常觉得被针对或被推诿，自我中心倾向较明显。建议尝试换位思考，增强人际沟通，必要时请心理专家协助调整认知模式。",
    "重度": "存在强烈的敌对猜疑、妄想倾向或关系观念，严重影响了对现实的检验能力。强烈建议在家属的陪同下，尽快前往精神专科医院就诊。"
  },
  "精神病性": {
    "正常": "您现实感良好，思维清晰，与现实生活保持着紧密的联系。",
    "轻度": "偶尔感到孤立无援或有些不切实际的幻想，多是极度疲劳或高压所致。建议保证充足休息，多参与现实生活中的社交互动。",
    "中度": "孤僻退缩、被控制感或其他异样思维体验开始出现，现实感有所减弱。请务必引起高度重视，尽早向专业心理或精神科医生咨询。",
    "重度": "出现明显的幻听、思维播散感或极度孤僻的生活方式，精神症状严重。请家属务必高度关注，立刻陪同前往精神专科医院接受系统治疗。"
  },
  "其他": {
    "正常": "您的睡眠和饮食规律正常，精力充沛，状态良好。",
    "轻度": "偶尔出现入睡困难、多梦或食欲不佳。建议调整作息，睡前避免过度兴奋，保持放松。",
    "中度": "睡眠和饮食问题较突出，已明显影响了白天的精力与情绪。建议规范作息习惯，必要时咨询医生获取辅助。",
    "重度": "严重的失眠或饮食障碍，极大危害了身体健康。建议立刻就医寻求专科干预。"
  }
};

Page({
  data: {
    report: {} as any,
    ec: {
      lazyLoad: true 
    }
  },

  ecComponent: null as any,

  onLoad() {
    // 1. 从缓存获取数据
    const resultData = wx.getStorageSync('currentResult');
    
    if (resultData) {
      // 2. 如果是 SCL90 量表，我们将智能建议动态塞入每一项的维度数据中
      if (resultData.reportType === 'SCL90' && resultData.dimensions) {
        resultData.dimensions.forEach((dim: any) => {
          const factorName = dim.name;
          const rating = dim.rating;
          
          // 在字典中匹配建议，如果匹配不到，给一个默认鼓励文案
          if (adviceDict[factorName] && adviceDict[factorName][rating]) {
            dim.adviceText = adviceDict[factorName][rating];
          } else {
            dim.adviceText = "请保持积极乐观的心态，关注身心健康。";
          }
        });
      }
      
      this.setData({ report: resultData });
    }
  },

  onReady() {
    if (this.data.report && this.data.report.reportType === 'SCL90') {
      this.ecComponent = this.selectComponent('#mychart-dom-radar');
      this.initRadarChart();
    }
  },

  initRadarChart() {
    const report = this.data.report;
    if (!report || !report.dimensions || !this.ecComponent) return;

    const indicatorArr: any[] = [];
    const dataValues: number[] = [];

    report.dimensions.forEach((dim: any) => {
      indicatorArr.push({ name: dim.name, max: 5 });
      dataValues.push(dim.avg);
    });

    this.ecComponent.init((canvas: any, width: number, height: number, dpr: number) => {
      const chart = echarts.init(canvas, null, {
        width: width,
        height: height,
        devicePixelRatio: dpr
      });

      const option = {
        color: ['#1890ff'], 
        radar: {
          radius: '55%', 
          indicator: indicatorArr,
          shape: 'circle',
          splitNumber: 5,
          name: {
            textStyle: { color: '#555', fontSize: 11 }
          },
          splitArea: {
            areaStyle: {
              color: ['rgba(24,144,255, 0.05)', 'rgba(24,144,255, 0.02)', '#fff', '#fff', '#fff'],
              shadowColor: 'rgba(0, 0, 0, 0.1)',
              shadowBlur: 10
            }
          },
          axisLine: { lineStyle: { color: '#eee' } },
          splitLine: { lineStyle: { color: '#eee' } }
        },
        series: [{
          name: '心理测评得分',
          type: 'radar',
          data: [{
            value: dataValues,
            name: '均分',
            areaStyle: { color: 'rgba(24,144,255, 0.3)' },
            lineStyle: { width: 2 }
          }]
        }]
      };

      chart.setOption(option);
      return chart;
    });
  },

  backToHome() {
    wx.reLaunch({
      url: '/pages/assessment/assessment'
    });
  },

  printReport() {
    const report = this.data.report;
    if (!report) {
      wx.showToast({ title: '报告数据为空', icon: 'none' });
      return;
    }

    // 收集填写人信息
    const userInfo = wx.getStorageSync('currentUser') || {};

    // 格式化日期兜底
    const rawDate = report.create_time || report.testDate || '';
    const formatDate = (d: string) => {
      if (!d) return '';
      if (/^\d{4}-\d{2}-\d{2}/.test(d)) return d.split(' ')[0];
      return d;
    };
    const today = (() => {
      const d = new Date();
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    })();

    const respondent = {
      nickname: report.nickname || userInfo.nickname || '未知',
      userId: report.userId || userInfo.id || '',
      testDate: formatDate(rawDate) || today
    };

    wx.showLoading({ title: '正在生成PDF...' });

    request({
      url: 'https://botany-refined-pleading.ngrok-free.dev/api/print/report',
      method: 'POST',
      data: {
        report: report,
        respondent: respondent
      },
      responseType: 'arraybuffer',
      success: (res: any) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          const fs = wx.getFileSystemManager();
          const filePath = `${wx.env.USER_DATA_PATH}/report_${Date.now()}.pdf`;

          fs.writeFile({
            filePath: filePath,
            data: res.data,
            success: () => {
              // 提示用户并打开 PDF
              wx.showModal({
                title: '报告已生成',
                content: '点击「查看报告」即可预览。\n在预览页点击右上角「···」菜单可选择「保存到手机」或「转发给朋友」。',
                confirmText: '查看报告',
                cancelText: '稍后查看',
                success: (modalRes: any) => {
                  if (modalRes.confirm) {
                    this.openPdf(filePath);
                  }
                }
              });
            },
            fail: (err: any) => {
              console.error('写入文件失败', err);
              if (res.data && typeof res.data === 'string') {
                this.downloadAndOpenPdf(res.data);
              }
            }
          });
        } else {
          wx.showToast({ title: '生成PDF失败', icon: 'none' });
        }
      },
      fail: (err: any) => {
        wx.hideLoading();
        console.error('打印请求失败', err);
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  },

  openPdf(filePath: string) {
    wx.openDocument({
      filePath: filePath,
      fileType: 'pdf',
      success: () => {
        console.log('PDF 打开成功');
      },
      fail: (err: any) => {
        console.error('打开 PDF 失败', err);
        wx.showToast({ title: '打开失败，请重试', icon: 'none' });
      }
    });
  },

  downloadAndOpenPdf(url: string) {
    wx.downloadFile({
      url: url,
      success: (res: any) => {
        if (res.statusCode === 200) {
          const filePath = res.tempFilePath;
          wx.showModal({
            title: '报告已生成',
            content: '点击「查看报告」即可预览。\n在预览页点击右上角「···」菜单可选择「保存到手机」或「转发给朋友」。',
            confirmText: '查看报告',
            cancelText: '稍后查看',
            success: (modalRes: any) => {
              if (modalRes.confirm) {
                this.openPdf(filePath);
              }
            }
          });
        }
      },
      fail: (err: any) => {
        console.error('下载 PDF 失败', err);
        wx.showToast({ title: '下载失败', icon: 'none' });
      }
    });
  }
})