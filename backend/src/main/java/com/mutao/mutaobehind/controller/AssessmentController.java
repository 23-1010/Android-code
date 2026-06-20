package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.entity.Assessment;
import com.mutao.mutaobehind.mapper.AssessmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class AssessmentController {

    @Autowired
    private AssessmentMapper assessmentMapper;

    // 当访问 /assessments 网址时，返回所有测评表数据
    @GetMapping("/assessments")
    public List<Assessment> getAssessments() {
        return assessmentMapper.getAllAssessments();
    }

    // 👇 这是你要【新增】的：提供单个量表的详情查询
    @GetMapping("/assessment")
    public Assessment getAssessment(Long id) {
        return assessmentMapper.getAssessmentById(id);
    }
}