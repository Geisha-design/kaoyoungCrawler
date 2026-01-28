package smartebao.guide.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerUser;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.mapper.CrawlerUserMapper;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.utils.JwtUtil;
import smartebao.guide.utils.LogUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "客户端登陆状态管理", description = "用于管理客户端登陆/注册/登出状态的API")
@Slf4j
public class LoginController {

    @Autowired
    private CrawlerUserMapper userMapper;

    @Autowired
    private CrawlerClientMapper clientMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "login", credentials.get("username"));
        
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            // 验证用户名和密码
            QueryWrapper<CrawlerUser> wrapper = new QueryWrapper<>();
            wrapper.eq("username", username).eq("password", password);
            CrawlerUser user = userMapper.selectOne(wrapper);

            if (user != null) {
                // 生成JWT令牌
                String token = jwtUtil.generateToken(username);

                // 在登录时同时绑定或更新crawler_client表
                String clientId = credentials.get("clientId"); // 从前端获取客户端ID
                LogUtils.logInfo("客户端ID: " + clientId);

                if (clientId != null && !clientId.isEmpty()) {
                    // 尝试查找现有的客户端记录
                    QueryWrapper<CrawlerClient> clientWrapper = new QueryWrapper<>();
                    clientWrapper.eq("client_id", clientId);
                    clientWrapper.eq("status", "offline");
                    clientWrapper.eq("username", username);
                    CrawlerClient existingClient = clientMapper.selectOne(clientWrapper);

                    if (existingClient != null) {
                        // 更新现有客户端记录
                        existingClient.setUsername(username);
                        existingClient.setStatus("online");
                        existingClient.setLastUpdateTime(new Date());
                        clientMapper.updateById(existingClient);
                        
                        LogUtils.logInfo("更新现有客户端记录 - clientId: " + clientId + ", username: " + username);
                    } else {
                        // 创建新的客户端记录
                        CrawlerClient newClient = new CrawlerClient();
                        newClient.setClientId(clientId);
                        newClient.setUsername(username);
                        newClient.setConnectTime(new Date());
                        newClient.setStatus("online");
                        newClient.setLastUpdateTime(new Date());
                        newClient.setIdleStatus(false); // 默认非空闲状态
                        clientMapper.insert(newClient);
                        
                        LogUtils.logInfo("创建新客户端记录 - clientId: " + clientId + ", username: " + username);
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("code", 200);
                response.put("message", "登录成功");
                Map<String, String> tokenData = new HashMap<>();
                tokenData.put("token", token);
                response.put("data", tokenData);

                LogUtils.logInfo("用户登录成功 - username: " + username);
                LogUtils.logMethodExit(this.getClass().getSimpleName(), "login", "Login successful");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 401);
                response.put("message", "用户名或密码错误");

                LogUtils.logWarning("用户登录失败 - 用户名或密码错误, username: " + username);
                LogUtils.logMethodExit(this.getClass().getSimpleName(), "login", "Login failed");
                return ResponseEntity.status(401).body(response);
            }
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userData) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "register", userData.get("username"));
        
        try {
            String username = userData.get("username");
            String password = userData.get("password");
            String clientId = userData.get("clientId"); // 获取客户端ID

            // 检查用户名是否已存在
            QueryWrapper<CrawlerUser> wrapper = new QueryWrapper<>();
            wrapper.eq("username", username);
            CrawlerUser existingUser = userMapper.selectOne(wrapper);

            if (existingUser != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 409); // 冲突状态码
                response.put("message", "用户名已存在");

                LogUtils.logWarning("用户注册失败 - 用户名已存在, username: " + username);
                LogUtils.logMethodExit(this.getClass().getSimpleName(), "register", "Registration failed - username exists");
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
            LogUtils.logInfo("创建新用户 - username: " + username);

            // 如果提供了clientId，在crawler_client表中创建记录，但是在注册的时候应该是未登陆在线的状态
            if (clientId != null && !clientId.isEmpty()) {
                CrawlerClient newClient = new CrawlerClient();
                newClient.setClientId(clientId);
                newClient.setUsername(username);
                newClient.setConnectTime(new Date());
                newClient.setStatus("offline");
                newClient.setLastUpdateTime(new Date());
                newClient.setIdleStatus(false); // 默认非空闲状态
                clientMapper.insert(newClient);
                
                LogUtils.logInfo("创建新客户端记录（离线状态）- clientId: " + clientId + ", username: " + username);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "注册成功");

            LogUtils.logInfo("用户注册成功 - username: " + username);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "register", "Registration successful");
            return ResponseEntity.ok(response);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> requestData) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "logout", token);
        
        try {
            // 验证JWT令牌
            String jwtToken = token.substring(7); // 移除"Bearer "前缀
            String username = jwtUtil.getUsernameFromToken(jwtToken);
            
            if (username == null || !jwtUtil.validateToken(jwtToken)) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 401);
                response.put("message", "无效的令牌");
                
                LogUtils.logWarning("用户登出失败 - 无效的令牌");
                LogUtils.logMethodExit(this.getClass().getSimpleName(), "logout", "Logout failed - invalid token");
                return ResponseEntity.status(401).body(response);
            }
            
            // 获取客户端ID
            String clientId = requestData.get("clientId");
            
            if (clientId != null && !clientId.isEmpty()) {
                // 更新客户端状态为离线
                QueryWrapper<CrawlerClient> clientWrapper = new QueryWrapper<>();
                clientWrapper.eq("client_id", clientId);
                clientWrapper.eq("username", username);
                CrawlerClient existingClient = clientMapper.selectOne(clientWrapper);
                
                if (existingClient != null) {
                    existingClient.setStatus("offline");
                    existingClient.setLastUpdateTime(new Date());
                    clientMapper.updateById(existingClient);
                    
                    LogUtils.logInfo("更新客户端状态为离线 - clientId: " + clientId + ", username: " + username);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "退出登录成功");
            
            LogUtils.logInfo("用户登出成功 - username: " + username);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "logout", "Logout successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogUtils.logError("用户登出时发生异常", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "退出登录失败: " + e.getMessage());
            
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "logout", "Logout failed - exception occurred");
            return ResponseEntity.status(500).body(response);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }
}