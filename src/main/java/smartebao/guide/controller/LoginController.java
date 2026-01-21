package smartebao.guide.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerUser;
import smartebao.guide.mapper.CrawlerUserMapper;
import smartebao.guide.utils.JwtUtil;

import java.util.Date;
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
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("token", token);
            response.put("data", tokenData);

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 401);
            response.put("message", "用户名或密码错误");

            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userData) {
        String username = userData.get("username");
        String password = userData.get("password");

        // 检查用户名是否已存在
        QueryWrapper<CrawlerUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        CrawlerUser existingUser = userMapper.selectOne(wrapper);

        if (existingUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 409); // 冲突状态码
            response.put("message", "用户名已存在");

            return ResponseEntity.status(409).body(response);
        }

        // 创建新用户
        CrawlerUser newUser = new CrawlerUser();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setCreateTime(new Date());
        newUser.setUpdateTime(new Date());

        // 保存用户到数据库
        userMapper.insert(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "注册成功");

        return ResponseEntity.ok(response);
    }
}