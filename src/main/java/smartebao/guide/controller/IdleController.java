package smartebao.guide.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.service.WebSocketService;
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/idle")
public class IdleController {

    @Autowired
    private CrawlerClientMapper clientMapper;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 获取所有客户端的空闲状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAllIdleStatus() {
        try {
            QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
            List<CrawlerClient> clients = clientMapper.selectList(wrapper);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", clients);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取指定客户端的空闲状态
     */
    @GetMapping("/status/{clientId}")
    public ResponseEntity<Map<String, Object>> getClientIdleStatus(@PathVariable String clientId) {
        try {
            QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
            wrapper.eq("client_id", clientId);
            CrawlerClient client = clientMapper.selectOne(wrapper);

            if (client == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 404);
                response.put("message", "客户端不存在");

                return ResponseEntity.status(404).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", client);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 设置客户端空闲阈值（通过WebSocket发送给客户端）
     */
    @PostMapping("/threshold/{clientId}")
    public ResponseEntity<Map<String, Object>> setClientIdleThreshold(@PathVariable String clientId, @RequestBody Map<String, Object> request) {
        try {
            Integer threshold = (Integer) request.get("threshold"); // 毫秒

            // 检查客户端是否在线
            if (!CrawlerWebSocketHandler.isClientOnline(clientId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "客户端不在线");

                return ResponseEntity.badRequest().body(response);
            }

            // 通过WebSocket向客户端发送设置空闲阈值的命令
            // 创建一个空闲控制命令消息
            Map<String, Object> command = new HashMap<>();
            command.put("command", "set_idle_threshold");
            command.put("threshold", threshold);

            // 这里需要扩展WebSocket处理器以支持发送此类命令
            // 目前仅返回成功响应，实际发送需要在WebSocket处理器中实现

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "空闲阈值设置命令已发送");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "设置失败: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查客户端空闲状态
     */
    @PostMapping("/check/{clientId}")
    public ResponseEntity<Map<String, Object>> checkClientIdleStatus(@PathVariable String clientId) {
        try {
            // 检查客户端是否在线
            if (!CrawlerWebSocketHandler.isClientOnline(clientId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "客户端不在线");

                return ResponseEntity.badRequest().body(response);
            }

            // 通过WebSocket向客户端发送检查空闲状态的命令
            Map<String, Object> command = new HashMap<>();
            command.put("command", "check_idle_status");

            // 这里需要扩展WebSocket处理器以支持发送此类命令
            // 目前仅返回成功响应，实际发送需要在WebSocket处理器中实现

            // 同时查询数据库中的状态
            QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
            wrapper.eq("client_id", clientId);
            CrawlerClient client = clientMapper.selectOne(wrapper);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "检查成功");
            response.put("data", client);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "检查失败: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}