package smartebao.guide.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerUser;
import smartebao.guide.mapper.CrawlerUserMapper;
import smartebao.guide.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private CrawlerUserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // 验证用户名和密码
        QueryWrapper<CrawlerUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("password", password);
        CrawlerUser user = userMapper.selectOne(wrapper);

        if (user != null) {
            // 生成JWT令牌
            String token = jwtUtil.generateToken(username);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录成功");
            response.put("data", Map.of("token", token));

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 401);
            response.put("message", "用户名或密码错误");

            return ResponseEntity.status(401).body(response);
        }
    }
}