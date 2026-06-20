package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户个人资料（学生端使用）
 *
 * GET  /api/user/profile?userId=xxx
 * PUT  /api/user/profile
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * GET /api/user/profile?userId=xxx
     * 获取用户个人资料（含年龄）
     */
    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> profile = sysUserMapper.getUserProfile(userId);
            if (profile != null) {
                result.putAll(profile);
            } else {
                result.put("error", "用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * PUT /api/user/profile
     * Body: { userId, realName, gender, birthDate }
     * 学生更新自己的个人资料
     */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = body.get("userId") != null
                    ? Long.valueOf(body.get("userId").toString()) : null;

            if (userId == null) {
                result.put("code", 400);
                result.put("error", "userId 不能为空");
                return result;
            }

            String realName = (String) body.getOrDefault("realName", "");
            String gender = (String) body.getOrDefault("gender", "");
            String birthDate = (String) body.getOrDefault("birthDate", null);

            int rows = sysUserMapper.updateUserProfile(userId, realName, gender, birthDate);
            result.put("code", rows > 0 ? 200 : 404);
            result.put("message", rows > 0 ? "保存成功" : "用户不存在");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("error", "更新失败：" + e.getMessage());
        }
        return result;
    }
}
