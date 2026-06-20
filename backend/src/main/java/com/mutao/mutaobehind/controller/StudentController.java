package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.mapper.AssessmentRecordMapper;
import com.mutao.mutaobehind.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 模块3：学生管理
 *
 * GET /api/teacher/students?teacherId=xxx
 * GET /api/teacher/students?counselorId=xxx  （fallback：通过 counselors.id 反查老师用户）
 * GET /api/teacher/students/:id/records
 * GET /api/teacher/students/:id/profile
 * PUT /api/teacher/students/:id/notes
 */
@RestController
@RequestMapping("/api/teacher")
public class StudentController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AssessmentRecordMapper recordMapper;

    /**
     * GET /api/teacher/students
     *   ?teacherId=xxx    → 标准路径
     *   ?counselorId=xxx  → fallback：通过 counselors.id 找到对应的 users.id
     */
    @GetMapping("/students")
    public Map<String, Object> getStudents(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long counselorId) {

        Map<String, Object> result = new HashMap<>();
        try {
            // 解析 teacherId
            Long resolvedTeacherId = teacherId;
            if (resolvedTeacherId == null && counselorId != null) {
                // Fallback：通过 counselor_id 在 users 表中找到老师用户
                resolvedTeacherId = sysUserMapper.getTeacherIdByCounselorId(counselorId);
            }

            if (resolvedTeacherId == null) {
                result.put("error", "请提供 teacherId 或 counselorId 参数");
                return result;
            }

            List<Map<String, Object>> students = sysUserMapper.getStudentsByTeacherId(counselorId, resolvedTeacherId);
            result.put("students", students);
            result.put("total", students.size());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * GET /api/teacher/students/:id/records
     * 获取某个学生的测评历史 + 学生个人资料
     */
    @GetMapping("/students/{id}/records")
    public Map<String, Object> getStudentRecords(@PathVariable Long id,
                                                  @RequestParam(required = false) Long teacherId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> records = sysUserMapper.getStudentAssessmentRecords(id);
            result.put("studentId", id);
            result.put("records", records);
            result.put("total", records.size());

            // 附带学生个人资料
            Map<String, Object> profile = sysUserMapper.getUserProfile(id);
            if (profile != null) {
                result.put("profile", profile);
            }

            // 附带老师备注（如果有 teacherId）
            if (teacherId != null) {
                String notes = sysUserMapper.getTeacherNotes(teacherId, id);
                result.put("teacherNotes", notes != null ? notes : "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * GET /api/teacher/students/:id/profile?teacherId=xxx
     * 获取学生个人资料 + 老师备注
     */
    @GetMapping("/students/{id}/profile")
    public Map<String, Object> getStudentProfile(@PathVariable Long id,
                                                  @RequestParam Long teacherId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> profile = sysUserMapper.getUserProfile(id);
            if (profile != null) {
                result.putAll(profile);
            } else {
                result.put("error", "学生不存在");
                return result;
            }

            String notes = sysUserMapper.getTeacherNotes(teacherId, id);
            result.put("teacherNotes", notes != null ? notes : "");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * PUT /api/teacher/students/:id/notes
     * Body: { teacherId, notes }
     * 老师更新对学生的备注
     */
    @PutMapping("/students/{id}/notes")
    public Map<String, Object> updateStudentNotes(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long teacherId = body.get("teacherId") != null
                    ? Long.valueOf(body.get("teacherId").toString()) : null;

            if (teacherId == null) {
                result.put("code", 400);
                result.put("error", "teacherId 不能为空");
                return result;
            }

            String notes = (String) body.getOrDefault("notes", "");

            int rows = sysUserMapper.upsertTeacherNotes(teacherId, id, notes);
            result.put("code", rows > 0 ? 200 : 500);
            result.put("message", rows > 0 ? "保存成功" : "保存失败");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("error", "更新失败：" + e.getMessage());
        }
        return result;
    }
}
