package com.mutao.mutaobehind.dto;

import java.util.Map;

/**
 * POST /api/print/report 请求体
 * 与前端 printReport() 发送的结构完全一致
 */
public class PrintReportRequest {

    /** /calculate 返回的完整报告数据 */
    private Map<String, Object> report;

    /** 填写人信息 */
    private Respondent respondent;

    public Map<String, Object> getReport() { return report; }
    public void setReport(Map<String, Object> report) { this.report = report; }

    public Respondent getRespondent() { return respondent; }
    public void setRespondent(Respondent respondent) { this.respondent = respondent; }

    public static class Respondent {
        private String nickname;
        private Long userId;
        private String testDate;
        private String scaleName;

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getTestDate() { return testDate; }
        public void setTestDate(String testDate) { this.testDate = testDate; }

        public String getScaleName() { return scaleName; }
        public void setScaleName(String scaleName) { this.scaleName = scaleName; }
    }
}
