package smartebao.guide.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerScript;
import smartebao.guide.service.CrawlerScriptService;
import smartebao.guide.service.WebSocketService;
import smartebao.guide.utils.ResponseData;
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端脚本执行控制器 - 提供指定客户端执行脚本的功能
 */
@Tag(name = "客户端脚本执行接口", description = "用于指定客户端执行脚本库中的脚本")
@RestController
@RequestMapping("/api/client-script")
public class ClientScriptController {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private CrawlerScriptService scriptService;

    /**
     * 指定客户端执行脚本库中的脚本
     */
    @Operation(summary = "指定客户端执行脚本库中的脚本", description = "指定客户端ID和脚本ID，执行脚本库中的脚本")
    @PostMapping("/execute")
    public ResponseData executeScriptOnClient(@RequestBody Map<String, String> requestBody) {
        try {
            String clientId = requestBody.get("clientId");
            String scriptId = requestBody.get("scriptId");

            if (clientId == null || clientId.trim().isEmpty()) {
                return ResponseData.error("客户端ID不能为空");
            }

            if (scriptId == null || scriptId.trim().isEmpty()) {
                return ResponseData.error("脚本ID不能为空");
            }

            // 验证客户端是否在线
            if (!webSocketService.isClientConnected(clientId)) {
                return ResponseData.error("指定的客户端不在线: " + clientId);
            }

            // 验证脚本是否存在
            CrawlerScript script = scriptService.getById(scriptId);
            if (script == null) {
                return ResponseData.error("脚本不存在: " + scriptId);
            }

            // 通过WebSocket处理器向指定客户端发送执行脚本命令
            CrawlerWebSocketHandler handler = new CrawlerWebSocketHandler();
            String taskId = "manual_task_" + System.currentTimeMillis();
            handler.sendExecuteScriptCommand(clientId, taskId, scriptId, script.getContent());

            Map<String, Object> result = new HashMap<>();
            result.put("clientId", clientId);
            result.put("scriptId", scriptId);
            result.put("scriptName", script.getDescription());
            result.put("taskId", taskId);

            return ResponseData.success("脚本已发送至指定客户端执行", result);
        } catch (Exception e) {
            return ResponseData.error("执行脚本失败: " + e.getMessage());
        }
    }

    /**
     * 指定客户端执行脚本库中的脚本（通过路径参数）
     */
    @Operation(summary = "指定客户端执行脚本库中的脚本（路径参数）", description = "通过路径参数指定客户端ID和脚本ID，执行脚本库中的脚本")
    @PostMapping("/clients/{clientId}/scripts/{scriptId}/execute")
    public ResponseData executeScriptOnClientByPath(@Parameter(description = "客户端ID") @PathVariable String clientId,
                                                   @Parameter(description = "脚本ID") @PathVariable String scriptId) {
        try {
            // 验证客户端是否在线
            if (!webSocketService.isClientConnected(clientId)) {
                return ResponseData.error("指定的客户端不在线: " + clientId);
            }

            // 验证脚本是否存在
            CrawlerScript script = scriptService.getById(scriptId);
            if (script == null) {
                return ResponseData.error("脚本不存在: " + scriptId);
            }

            // 通过WebSocket处理器向指定客户端发送执行脚本命令
            CrawlerWebSocketHandler handler = new CrawlerWebSocketHandler();
            String taskId = "manual_task_" + System.currentTimeMillis();
            handler.sendExecuteScriptCommand(clientId, taskId, scriptId, script.getContent());

            Map<String, Object> result = new HashMap<>();
            result.put("clientId", clientId);
            result.put("scriptId", scriptId);
            result.put("scriptName", script.getDescription());
            result.put("taskId", taskId);

            return ResponseData.success("脚本已发送至指定客户端执行", result);
        } catch (Exception e) {
            return ResponseData.error("执行脚本失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有可用脚本
     */
    @Operation(summary = "获取所有可用脚本", description = "获取脚本库中所有可用的脚本")
    @GetMapping("/scripts")
    public ResponseData getAllScripts() {
        try {
            return ResponseData.success("获取脚本列表成功", scriptService.list());
        } catch (Exception e) {
            return ResponseData.error("获取脚本列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定客户端的在线状态
     */
    @Operation(summary = "获取指定客户端的在线状态", description = "检查指定客户端是否在线")
    @GetMapping("/clients/{clientId}/status")
    public ResponseData getClientStatus(@Parameter(description = "客户端ID") @PathVariable String clientId) {
        try {
            boolean isConnected = webSocketService.isClientConnected(clientId);
            Map<String, Object> status = new HashMap<>();
            status.put("clientId", clientId);
            status.put("isConnected", isConnected);

            if (isConnected) {
                return ResponseData.success("客户端在线", status);
            } else {
                return ResponseData.error("客户端不在线");
            }
        } catch (Exception e) {
            return ResponseData.error("检查客户端状态失败: " + e.getMessage());
        }
    }
}