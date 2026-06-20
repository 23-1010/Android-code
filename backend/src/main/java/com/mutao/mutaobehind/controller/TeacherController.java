package com.mutao.mutaobehind.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mutao.mutaobehind.mapper.AssessmentRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TeacherController {

    @Autowired
    private AssessmentRecordMapper recordMapper;

    // 供小程序老师端拉取所有成绩报表
    @GetMapping("/api/teacher/records")
    public List<Map<String, Object>> getAllStudentRecords() {
        return recordMapper.getAllRecordsForTeacher();
    }

    // 【新增】根据记录 ID 返回该答卷的完整评估详情，返回格式与 /calculate 一致
    @GetMapping("/api/teacher/records/{recordId}")
    public Map<String, Object> getRecordDetail(@PathVariable Long recordId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> record = recordMapper.getRecordById(recordId);
            if (record == null) {
                result.put("error", "记录不存在");
                return result;
            }
            String reportJson = (String) record.get("report_json");
            if (reportJson == null || reportJson.isEmpty()) {
                result.put("error", "该记录为旧数据，无完整报告");
                return result;
            }
            // 反序列化返回，格式与 /calculate 完全一致，前端不用改
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(reportJson, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
            return result;
        }
    }
}