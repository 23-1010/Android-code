package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.entity.Counselor;
import com.mutao.mutaobehind.mapper.CounselorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块1：老师个人信息
 *
 * GET  /api/teacher/profile?teacherId=xxx
 * GET  /api/teacher/profile?counselorId=xxx  （fallback：直接按咨询师ID查）
 *
 * PUT  /api/teacher/profile
 *   — 更新 counselors 表对应字段
 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherProfileController {

    @Autowired
    private CounselorMapper counselorMapper;

    /**
     * GET /api/teacher/profile
     *   ?teacherId=xxx    → 通过 counselors.user_id 查询
     *   ?counselorId=xxx  → 直接按 counselors.id 查询（fallback / dev 模式）
     */
    @GetMapping("/profile")
    public Map<String, Object> getProfile(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long counselorId) {

        Map<String, Object> result = new HashMap<>();
        try {
            Counselor counselor = null;

            if (teacherId != null) {
                // 标准路径：通过 user_id 查 counselors
                counselor = counselorMapper.getCounselorByUserId(teacherId);
            } else if (counselorId != null) {
                // Fallback：直接用 counselors.id 查
                counselor = counselorMapper.getCounselorById(counselorId);
            } else {
                result.put("error", "请提供 teacherId 或 counselorId 参数");
                return result;
            }

            if (counselor != null) {
                result.put("id", counselor.getId());
                result.put("name", counselor.getName());
                result.put("title", counselor.getTitle());
                result.put("avatar", counselor.getAvatar());
                result.put("shortDesc", counselor.getShortDesc());
                result.put("fullDesc", counselor.getFullDesc());
                result.put("specialties", counselor.getSpecialties());
                result.put("phone", counselor.getPhone());
            } else {
                result.put("id", null);
                result.put("name", "");
                result.put("title", "");
                result.put("avatar", "");
                result.put("shortDesc", "");
                result.put("fullDesc", "");
                result.put("specialties", "");
                result.put("phone", "");
                result.put("message", "未找到咨询师资料");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * PUT /api/teacher/profile
     * Body: { teacherId, shortDesc, fullDesc, specialties, phone, email }
     * 返回：{ code: 200, message: "ok" }
     */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long teacherId = body.get("teacherId") != null
                    ? Long.valueOf(body.get("teacherId").toString()) : null;

            if (teacherId == null) {
                result.put("code", 400);
                result.put("error", "teacherId 不能为空");
                return result;
            }

            Counselor counselor = new Counselor();
            counselor.setUserId(teacherId);
            counselor.setShortDesc((String) body.getOrDefault("shortDesc", ""));
            counselor.setFullDesc((String) body.getOrDefault("fullDesc", ""));
            counselor.setSpecialties((String) body.getOrDefault("specialties", ""));
            counselor.setPhone((String) body.getOrDefault("phone", ""));
            counselor.setEmail((String) body.getOrDefault("email", ""));

            int rows = counselorMapper.updateProfileByUserId(counselor);
            if (rows > 0) {
                result.put("code", 200);
                result.put("message", "ok");
            } else {
                result.put("code", 404);
                result.put("error", "未找到对应咨询师记录（counselors.user_id = " + teacherId + "）");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("error", "更新失败：" + e.getMessage());
        }
        return result;
    }
}
