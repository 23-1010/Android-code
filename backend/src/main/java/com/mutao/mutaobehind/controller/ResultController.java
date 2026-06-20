package com.mutao.mutaobehind.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mutao.mutaobehind.entity.Assessment;
import com.mutao.mutaobehind.mapper.AssessmentMapper;
import com.mutao.mutaobehind.mapper.AssessmentRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ResultController {

    @Autowired
    private AssessmentMapper assessmentMapper;

    @Autowired
    private AssessmentRecordMapper recordMapper;

    @PostMapping("/calculate")
    public Map<String, Object> calculateScore(@RequestBody SubmitDto submitDto) {
        Map<String, Object> finalResult = new HashMap<>();

        try {
            Assessment assessment = assessmentMapper.getAssessmentById(submitDto.getAssessmentId());
            if (assessment == null) return finalResult;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(assessment.getQuestionsJson());
            JsonNode questionsNode = rootNode.get("questions");
            Map<String, Integer> userAnswers = submitDto.getAnswers();

            if (userAnswers.isEmpty()) return finalResult;

            // 1. 获取量表类型标签，直接读取数据库里的独立字段
            String scaleType = assessment.getScaleType() != null ? assessment.getScaleType() : "SCL90";
            finalResult.put("reportType", scaleType);

            // 2. 基础数据统计：算出原始总分
            int totalRawScore = 0;
            for (JsonNode qNode : questionsNode) {
                String qId = qNode.get("id").asText();
                if (userAnswers.containsKey(qId)) {
                    totalRawScore += userAnswers.get(qId);
                }
            }

            // 3. 【核心策略分发】：根据不同量表走不同的计分逻辑
            if ("SCL90".equals(scaleType)) {
                calculateSCL90(questionsNode, userAnswers, totalRawScore, finalResult);
            } else if ("SDS".equals(scaleType)) {
                calculateSDS(totalRawScore, finalResult);
            }

            // 👇 4. 【新增】：从上面的计算结果中，提取出最终的得分和评级，准备存入数据库
            double finalScore = 0.0;
            String finalRating = "未知";

            if ("SCL90".equals(scaleType)) {
                // SCL90 取总分和阴阳性作为入库指标
                finalScore = ((Number) finalResult.get("totalScore")).doubleValue();
                finalRating = (String) finalResult.get("isPositive");
            } else if ("SDS".equals(scaleType)) {
                // SDS 取标准分和抑郁程度作为入库指标
                finalScore = ((Number) finalResult.get("standardScore")).doubleValue();
                finalRating = (String) finalResult.get("rating");
            }

            // 👇 5. 【入库】：如果前端传了 userId，就把刚刚提取的成绩写入数据库！
            if (submitDto.getUserId() != null) {
                String reportJson = mapper.writeValueAsString(finalResult);
                recordMapper.insertRecord(submitDto.getUserId(), scaleType, assessment.getTitle(), finalScore, finalRating, reportJson);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalResult;
    }

    // ------- SCL-90 的专属计算逻辑 (复杂多维度) -------
    private void calculateSCL90(JsonNode questionsNode, Map<String, Integer> userAnswers, int totalScore, Map<String, Object> finalResult) {
        Map<String, Double> dimTotalScores = new HashMap<>();
        Map<String, Integer> dimCounts = new HashMap<>();
        int positiveCount = 0;

        for (JsonNode qNode : questionsNode) {
            String qId = qNode.get("id").asText();
            String dimension = qNode.has("dimension") ? qNode.get("dimension").asText() : "其它";

            if (userAnswers.containsKey(qId)) {
                int score = userAnswers.get(qId);
                if (score >= 2) positiveCount++;
                dimTotalScores.put(dimension, dimTotalScores.getOrDefault(dimension, 0.0) + score);
                dimCounts.put(dimension, dimCounts.getOrDefault(dimension, 0) + 1);
            }
        }

        double overallAvg = Math.round((totalScore / (double) userAnswers.size()) * 100.0) / 100.0;
        String isPositive = (totalScore > 160 || positiveCount > 43) ? "阳性" : "阴性";

        List<Map<String, Object>> dimensionsList = new ArrayList<>();
        for (String dim : dimTotalScores.keySet()) {
            Map<String, Object> dimData = new HashMap<>();
            double total = dimTotalScores.get(dim);
            double avg = Math.round((total / dimCounts.get(dim)) * 100.0) / 100.0;
            dimData.put("name", dim);
            dimData.put("total", total);
            dimData.put("avg", avg);
            dimData.put("rating", getSCL90Rating(avg));
            dimensionsList.add(dimData);
        }

        finalResult.put("totalScore", totalScore);
        finalResult.put("overallAvg", overallAvg);
        finalResult.put("positiveCount", positiveCount);
        finalResult.put("isPositive", isPositive);
        finalResult.put("overallRating", getSCL90Rating(overallAvg));
        finalResult.put("dimensions", dimensionsList);
    }

    // ------- SDS (抑郁自评量表) 的专属计算逻辑 (标准总分制) -------
    private void calculateSDS(int rawScore, Map<String, Object> finalResult) {
        // SDS 规则：标准分 = 原始总分 * 1.25，取整数部分
        int standardScore = (int) (rawScore * 1.25);
        String rating = "正常";

        if (standardScore >= 70) {
            rating = "重度抑郁";
        } else if (standardScore >= 60) {
            rating = "中度抑郁";
        } else if (standardScore >= 50) {
            rating = "轻度抑郁";
        }

        finalResult.put("rawScore", rawScore);
        finalResult.put("standardScore", standardScore);
        finalResult.put("rating", rating);
    }

    private String getSCL90Rating(double avg) {
        if (avg < 2) return "正常";
        if (avg < 3) return "轻度";
        if (avg < 4) return "中度";
        return "重度";
    }
}

class SubmitDto {
    private Long assessmentId;
    private Long userId; // 必须要有这个，前端传过来的用户身份
    private Map<String, Integer> answers;

    public Long getAssessmentId() { return assessmentId; }
    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Map<String, Integer> getAnswers() { return answers; }
    public void setAnswers(Map<String, Integer> answers) { this.answers = answers; }
}