package smartebao.guide.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.entity.CrawlerScriptPushLog;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.mapper.CrawlerScriptPushLogMapper;
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/script")
public class ScriptController {

    @Autowired
    private CrawlerClientMapper clientMapper;

    @Autowired
    private CrawlerScriptPushLogMapper pushLogMapper;

    @PostMapping("/designated/push")
    public ResponseEntity<Map<String, Object>> designatedPush(@RequestBody Map<String, Object> request) {
        String clientId = (String) request.get("clientId");
        List<String> scriptIds = (List<String>) request.get("scriptIds");

        // 验证客户端是否存在
        QueryWrapper<CrawlerClient> clientWrapper = new QueryWrapper<>();
        clientWrapper.eq("client_id", clientId);
        CrawlerClient client = clientMapper.selectOne(clientWrapper);
        if (client == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", "客户端不存在");

            return ResponseEntity.badRequest().body(response);
        }

        // 检查客户端是否在线
        if (!CrawlerWebSocketHandler.isClientOnline(clientId)) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", "客户端不在线");

            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 调用WebSocket处理器定向下发脚本
            CrawlerWebSocketHandler handler = new CrawlerWebSocketHandler();
            handler.sendDesignatedScriptsToClient(clientId, scriptIds);

            // 记录下发日志
            CrawlerScriptPushLog log = new CrawlerScriptPushLog();
            log.setPushId("push_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
            log.setClientId(clientId);
            log.setScriptIds(String.join(",", scriptIds));
            log.setPushType("designated");
            log.setPushStatus("success");
            log.setPushTime(new Date());
            log.setRemark("定向下发脚本");
            pushLogMapper.insert(log);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "定向下发脚本请求已受理");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 记录失败日志
            CrawlerScriptPushLog log = new CrawlerScriptPushLog();
            log.setPushId("push_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
            log.setClientId(clientId);
            log.setScriptIds(String.join(",", scriptIds));
            log.setPushType("designated");
            log.setPushStatus("fail");
            log.setPushTime(new Date());
            log.setRemark("定向下发脚本失败: " + e.getMessage());
            pushLogMapper.insert(log);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "下发脚本失败: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}