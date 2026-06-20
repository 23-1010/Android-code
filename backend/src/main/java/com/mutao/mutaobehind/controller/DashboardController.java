package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.mapper.AppointmentMapper;
import com.mutao.mutaobehind.mapper.AssessmentRecordMapper;
import com.mutao.mutaobehind.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 模块4：数据监控大屏
 */
@RestController
@RequestMapping("/api/teacher/dashboard")
public class DashboardController {

    @Autowired
    private AssessmentRecordMapper recordMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * GET /api/teacher/dashboard/stats?teacherId=xxx
     * GET /api/teacher/dashboard/stats?counselorId=xxx  （fallback）
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long counselorId) {

        Map<String, Object> result = new HashMap<>();

        // 解析 teacherId
        Long resolvedTeacherId = teacherId;
        if (resolvedTeacherId == null && counselorId != null) {
            resolvedTeacherId = sysUserMapper.getTeacherUserIdByCounselorId(counselorId);
        }

        if (resolvedTeacherId == null) {
            result.put("error", "请提供 teacherId 或 counselorId 参数");
            return result;
        }

        try {
            Integer studentCount = recordMapper.countStudentsByTeacherId(resolvedTeacherId);
            result.put("totalStudents", studentCount != null ? studentCount : 0);

            Integer monthAssessmentCount = recordMapper.countMonthAssessmentsByTeacherId(resolvedTeacherId);
            result.put("monthAssessments", monthAssessmentCount != null ? monthAssessmentCount : 0);

            Integer warningCount = recordMapper.countWarningByTeacherId(resolvedTeacherId);
            result.put("warningCount", warningCount != null ? warningCount : 0);

            Integer monthAppointmentCount = appointmentMapper.countMonthAppointments(resolvedTeacherId);
            result.put("pendingAppointments", monthAppointmentCount != null ? monthAppointmentCount : 0);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }
}
