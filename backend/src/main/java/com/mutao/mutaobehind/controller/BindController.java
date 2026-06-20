package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.mapper.BindRequestMapper;
import com.mutao.mutaobehind.mapper.CounselorMapper;
import com.mutao.mutaobehind.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class BindController {

    @Autowired
    private CounselorMapper counselorMapper;

    @Autowired
    private BindRequestMapper bindRequestMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    // ==================== 老师端 ====================

    /**
     * GET /api/teacher/bind-code?counselorId=xxx
     * 获取或生成老师的4位绑定码
     */
    @GetMapping("/api/teacher/bind-code")
    public Map<String, Object> getBindCode(@RequestParam Long counselorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String code = counselorMapper.getBindCodeByCounselorId(counselorId);
            if (code == null || code.isEmpty()) {
                // 首次生成：4位随机数（1000-9999，排除顺序数字）
                code = generateUniqueCode(counselorId);
                counselorMapper.setBindCode(counselorId, code);
            }
            result.put("bindCode", code);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "获取绑定码失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * GET /api/teacher/bind-requests?counselorId=xxx
     * 老师获取待处理的绑定申请列表
     */
    @GetMapping("/api/teacher/bind-requests")
    public Map<String, Object> getBindRequests(@RequestParam Long counselorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> list = bindRequestMapper.getPendingByCounselorId(counselorId);
            result.put("requests", list);
            result.put("total", list.size());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * PUT /api/teacher/bind-requests/:id
     * Body: { status: "approved" | "rejected" }
     */
    @PutMapping("/api/teacher/bind-requests/{id}")
    public Map<String, Object> handleBindRequest(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String status = (String) body.get("status");
            if (!"approved".equals(status) && !"rejected".equals(status)) {
                result.put("code", 400);
                result.put("error", "status 必须是 approved 或 rejected");
                return result;
            }

            int rows = bindRequestMapper.updateStatus(id, status);
            if (rows == 0) {
                result.put("code", 404);
                result.put("error", "申请不存在");
                return result;
            }

            // 如果通过，同步写入 teacher_students 表
            if ("approved".equals(status)) {
                Map<String, Object> req = bindRequestMapper.getRequestById(id);
                if (req != null) {
                    Long studentId = Long.valueOf(req.get("studentId").toString());
                    Long counselorId = Long.valueOf(req.get("counselorId").toString());
                    // 通过 counselorId 反查 teacher 的 user_id
                    Long teacherUserId = sysUserMapper.getTeacherUserIdByCounselorId(counselorId);
                    if (teacherUserId != null) {
                        sysUserMapper.upsertTeacherNotes(teacherUserId, studentId, "");
                    }
                }
            }

            result.put("code", 200);
            result.put("message", "approved".equals(status) ? "已通过绑定" : "已拒绝申请");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("error", "操作失败：" + e.getMessage());
        }
        return result;
    }

    // ==================== 学生端 ====================

    /**
     * POST /api/student/bind-request
     * Body: { studentId, bindCode }
     */
    @PostMapping("/api/student/bind-request")
    public Map<String, Object> submitBindRequest(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long studentId = Long.valueOf(body.get("studentId").toString());
            String bindCode = (String) body.get("bindCode");

            if (bindCode == null || bindCode.trim().isEmpty()) {
                result.put("code", 400);
                result.put("error", "请输入绑定码");
                return result;
            }

            // 1. 查找对应咨询师
            Map<String, Object> counselor = counselorMapper.getCounselorByBindCode(bindCode.trim());
            if (counselor == null) {
                result.put("code", 404);
                result.put("error", "绑定码无效，未找到对应老师");
                return result;
            }
            Long counselorId = Long.valueOf(counselor.get("id").toString());

            // 2. 检查是否已有待处理的申请
            Map<String, Object> pending = bindRequestMapper.getPendingByStudentId(studentId);
            if (pending != null) {
                result.put("code", 409);
                result.put("error", "您已有一个待处理的绑定申请，请等待老师审核");
                return result;
            }

            // 3. 检查是否已经绑定过
            Map<String, Object> approved = bindRequestMapper.getApprovedTeacher(studentId);
            if (approved != null) {
                result.put("code", 409);
                result.put("error", "您已绑定了一位老师：" + approved.get("name"));
                return result;
            }

            // 4. 创建申请
            bindRequestMapper.insertBindRequest(studentId, counselorId);
            result.put("code", 200);
            result.put("message", "申请已发送，请等待老师审核");
            result.put("teacherName", counselor.get("name"));
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("error", "提交失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * GET /api/student/my-teacher?studentId=xxx
     * 学生查已绑定的老师信息
     */
    @GetMapping("/api/student/my-teacher")
    public Map<String, Object> getMyTeacher(@RequestParam Long studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> teacher = bindRequestMapper.getApprovedTeacher(studentId);
            if (teacher != null) {
                result.putAll(teacher);
                result.put("bound", true);
            } else {
                result.put("bound", false);

                // 检查是否有待处理申请
                Map<String, Object> pending = bindRequestMapper.getPendingByStudentId(studentId);
                result.put("pending", pending != null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    // ==================== 工具方法 ====================

    private String generateUniqueCode(Long counselorId) {
        // 生成4位随机数字，避免顺序数字（1234, 2345等）
        Random random = new Random();
        String code;
        int attempts = 0;
        do {
            int num = 1000 + random.nextInt(9000); // 1000-9999
            code = String.valueOf(num);
            // 排除简单顺序数字
            if (isSequential(code) && attempts < 50) {
                attempts++;
                continue;
            }
            break;
        } while (true);
        return code;
    }

    private boolean isSequential(String code) {
        if (code.length() != 4) return false;
        // 递增顺序：1234, 2345, 3456...
        boolean inc = true;
        // 递减顺序：4321, 5432, 6543...
        boolean dec = true;
        for (int i = 1; i < 4; i++) {
            if (code.charAt(i) != code.charAt(i - 1) + 1) inc = false;
            if (code.charAt(i) != code.charAt(i - 1) - 1) dec = false;
        }
        return inc || dec;
    }
}
