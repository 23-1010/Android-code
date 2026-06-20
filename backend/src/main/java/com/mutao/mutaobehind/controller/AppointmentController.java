package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.mapper.AppointmentMapper;
import com.mutao.mutaobehind.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块2：预约管理
 */
@RestController
@RequestMapping("/api")
public class AppointmentController {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * GET /api/appointments?month=2026-06&teacherId=xxx
     * GET /api/appointments?date=2026-06-19&teacherId=xxx
     * GET /api/appointments?date=2026-06-19&counselorId=xxx  （fallback）
     */
    @GetMapping("/appointments")
    public Map<String, Object> getAppointments(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long counselorId) {

        Map<String, Object> result = new HashMap<>();

        // 解析 teacherId：优先用 teacherId，否则通过 counselorId 反查
        Long resolvedTeacherId = teacherId;
        if (resolvedTeacherId == null && counselorId != null) {
            resolvedTeacherId = sysUserMapper.getTeacherUserIdByCounselorId(counselorId);
        }

        if (resolvedTeacherId == null) {
            result.put("error", "请提供 teacherId 或 counselorId 参数");
            return result;
        }

        try {
            if (month != null) {
                List<Map<String, Object>> dailyCounts = appointmentMapper.getCountByMonth(resolvedTeacherId, month);
                result.put("month", month);
                result.put("dailyCounts", dailyCounts);
            } else if (date != null) {
                List<Map<String, Object>> list = appointmentMapper.getByDate(resolvedTeacherId, date);
                result.put("date", date);
                result.put("appointments", list);
            } else {
                result.put("error", "请提供 month 或 date 参数");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * PUT /api/appointments/:id
     * 修改预约状态
     */
    @PutMapping("/appointments/{id}")
    public Map<String, Object> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String status = body.get("status");
            if (status == null || status.isEmpty()) {
                result.put("error", "status 不能为空");
                return result;
            }

            if (!status.matches("pending|confirmed|cancelled|completed")) {
                result.put("error", "status 值无效，可选: pending/confirmed/cancelled/completed");
                return result;
            }

            int rows = appointmentMapper.updateStatus(id, status);
            if (rows > 0) {
                result.put("message", "状态更新成功");
            } else {
                result.put("error", "预约不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "更新失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * POST /api/appointments
     * 学生端创建预约
     */
    @PostMapping("/appointments")
    public Map<String, Object> createAppointment(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long teacherId = body.get("teacherId") != null
                    ? Long.valueOf(body.get("teacherId").toString()) : null;
            String date = (String) body.get("appointmentDate");
            String timeSlot = (String) body.get("timeSlot");

            if (teacherId == null || date == null || timeSlot == null) {
                result.put("error", "teacherId, appointmentDate, timeSlot 为必填项");
                return result;
            }

            int conflict = appointmentMapper.countConflict(teacherId, date, timeSlot);
            if (conflict > 0) {
                result.put("error", "该时段已被预约，请选择其他时间");
                return result;
            }

            if (!body.containsKey("status")) {
                body.put("status", "pending");
            }

            appointmentMapper.insert(body);
            result.put("message", "预约成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "预约失败：" + e.getMessage());
        }
        return result;
    }
}
