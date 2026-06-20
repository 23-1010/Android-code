package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.entity.Counselor;
import com.mutao.mutaobehind.mapper.CounselorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 咨询师公开接口
 * GET /api/counselors     — 在职咨询师列表（status=1）
 * GET /api/counselors/:id — 咨询师详情
 */
@RestController
public class CounselorController {

    @Autowired
    private CounselorMapper counselorMapper;

    /**
     * GET /api/counselors
     * 返回所有 status=1（在职）的咨询师
     * 格式：纯数组 [{id, name, title, avatar, shortDesc, fullDesc, specialties, phone}, ...]
     */
    @GetMapping("/api/counselors")
    public List<Map<String, Object>> getCounselorList() {
        List<Counselor> counselors = counselorMapper.getAllCounselors();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Counselor c : counselors) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("title", c.getTitle());
            item.put("avatar", c.getAvatar());
            item.put("shortDesc", c.getShortDesc());
            item.put("fullDesc", c.getFullDesc());
            item.put("specialties", c.getSpecialties());
            item.put("phone", c.getPhone());
            list.add(item);
        }
        return list;
    }

    /**
     * GET /api/counselors/:id
     * 咨询师详情
     */
    @GetMapping("/api/counselors/{id}")
    public Map<String, Object> getCounselorDetail(@PathVariable("id") Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        Counselor c = counselorMapper.getCounselorById(id);
        if (c == null) {
            result.put("error", "咨询师不存在");
            return result;
        }
        result.put("id", c.getId());
        result.put("name", c.getName());
        result.put("title", c.getTitle());
        result.put("avatar", c.getAvatar());
        result.put("shortDesc", c.getShortDesc());
        result.put("fullDesc", c.getFullDesc());
        result.put("specialties", c.getSpecialties());
        result.put("phone", c.getPhone());
        return result;
    }
}
