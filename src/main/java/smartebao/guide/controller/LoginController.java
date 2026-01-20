package smartebao.guide.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerUser;
import smartebao.guide.service.CrawlerUserService;
import smartebao.guide.utils.JwtUtil;
import smartebao.guide.utils.ResponseData;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户登录管理", description = "用户认证相关的API")
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private CrawlerUserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "用户登录", description = "用户认证并获取JWT令牌")
    @PostMapping("/login")
    public ResponseData login(@RequestBody Map<String, String> loginInfo) {
        String username = loginInfo.get("username");
        String password = loginInfo.get("password");

        // 验证用户名和密码
        CrawlerUser user = userService.findByUsernameAndPassword(username, password);
        if (user != null) {
            // 生成JWT令牌
            String token = jwtUtil.generateToken(username);
            
            Map<String, String> result = new HashMap<>();
            result.put("token", token);
            result.put("username", username);
            
            return ResponseData.success("登录成功", result);
        } else {
            return ResponseData.error("用户名或密码错误");
        }
    }

    @Operation(summary = "用户注册", description = "创建新用户账户")
    @PostMapping("/register")
    public ResponseData register(@RequestBody CrawlerUser user) {
        // 检查用户是否已存在
        CrawlerUser existingUser = userService.findByUsername(user.getUsername());
        if (existingUser != null) {
            return ResponseData.error("用户名已存在");
        }

        // 保存新用户
        userService.save(user);
        return ResponseData.success("注册成功", user);
    }
}