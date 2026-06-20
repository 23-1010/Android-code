package com.mutao.mutaobehind.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mutao.mutaobehind.entity.SysUser;
import com.mutao.mutaobehind.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {

    @Autowired
    private SysUserMapper sysUserMapper;

    // 自动读取你在 yml 里配置的秘钥
    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @PostMapping("/api/wx/login")
    public Map<String, Object> wxLogin(@RequestBody Map<String, String> requestData) {
        Map<String, Object> response = new HashMap<>();
        String code = requestData.get("code");

        if (code == null || code.isEmpty()) {
            response.put("error", "未获取到code");
            return response;
        }

        // 1. 拼接向腾讯服务器请求的网址
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid +
                "&secret=" + secret + "&js_code=" + code + "&grant_type=authorization_code";

        try {
            // 2. 发起网络请求，呼叫腾讯服务器
            RestTemplate restTemplate = new RestTemplate();
            String wxResult = restTemplate.getForObject(url, String.class);

            // 3. 解析腾讯返回的 JSON 数据，拿到核心身份证明：openid
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(wxResult);
            String openid = rootNode.path("openid").asText();

            if (openid == null || openid.isEmpty()) {
                response.put("error", "微信登录验证失败");
                return response;
            }

            // 4. 去我们自己的数据库查，这人是不是注册过？
            SysUser user = sysUserMapper.getUserByOpenid(openid);

            // 5. 没注册过（第一次用小程序），就自动帮他注册，默认身份是 student
            if (user == null) {
                user = new SysUser();
                user.setOpenid(openid);
                sysUserMapper.insertUser(user);
            }

            // 6. 把用户的身份信息和权限发给前端小程序
            response.put("message", "登录成功");
            response.put("userInfo", user); // 里面包含了 id 和 role(student/teacher)

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "服务器内部错误");
        }
        return response;
    }
}