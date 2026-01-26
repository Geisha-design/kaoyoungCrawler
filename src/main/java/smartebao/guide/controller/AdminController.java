package smartebao.guide.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.entity.CrawlerScript;
import smartebao.guide.service.CrawlerClientService;
import smartebao.guide.service.CrawlerScriptService;
import smartebao.guide.service.WebSocketService;
import smartebao.guide.utils.ResponseData;
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * 管理端控制器 - 提供客户端管理、日志查看、统计分析等功能
 */
@Tag(name = "管理端接口", description = "管理员专用接口，用于客户端管理、日志查看、统计分析等")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private CrawlerClientService clientService;

    @Autowired
    private CrawlerScriptService scriptService;

    /**
     * 获取所有客户端状态
     */
    @Operation(summary = "获取所有客户端状态", description = "获取系统中所有客户端的状态信息")
    @GetMapping("/clients")
    public ResponseData getAllClients() {
        try {
            return webSocketService.getAllClientStatus();
        } catch (Exception e) {
            return ResponseData.error("获取客户端列表失败: " + e.getMessage());
        }
    }

    /**
     * 强制客户端下线（踢掉要求重新登录）
     */
    @Operation(summary = "强制客户端下线", description = "强制指定客户端下线，要求其重新登录")
    @PostMapping("/clients/{clientId}/kick")
    public ResponseData kickClient(@Parameter(description = "客户端ID") @PathVariable String clientId) {
        try {
            // 发送强制下线消息给客户端
            webSocketService.sendMessageToClient(clientId, "{\"type\":\"force_logout\",\"reason\":\"admin_kick\"}");
            
            // 更新客户端状态
            webSocketService.updateClientStatus(clientId, "offline");
            
            return ResponseData.success("客户端已被强制下线", clientId);
        } catch (Exception e) {
            return ResponseData.error("踢出客户端失败: " + e.getMessage());
        }
    }

    /**
     * 向指定客户端发送心跳请求
     */
    @Operation(summary = "向指定客户端发送心跳请求", description = "向指定客户端发送心跳检测请求")
    @PostMapping("/clients/{clientId}/heartbeat")
    public ResponseData sendHeartbeatRequest(@Parameter(description = "客户端ID") @PathVariable String clientId) {
        try {
            // 发送心跳请求给客户端
            webSocketService.sendMessageToClient(clientId, "{\"type\":\"ping\"}");
            
            return ResponseData.success("心跳请求已发送", clientId);
        } catch (Exception e) {
            return ResponseData.error("发送心跳请求失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端日志（简化版，实际实现可能需要从数据库或文件读取）
     */
    @Operation(summary = "获取客户端日志", description = "获取指定客户端的日志信息")
    @GetMapping("/clients/{clientId}/logs")
    public ResponseData getClientLogs(@Parameter(description = "客户端ID") @PathVariable String clientId) {
        try {
            // 这里只是一个示例，实际实现会从数据库或日志文件读取
            // 暂时返回客户端的基本信息作为日志示例
            CrawlerClient client = clientService.getById(clientId);
            if (client == null) {
                return ResponseData.error("客户端不存在");
            }
            
            return ResponseData.success("客户端日志", client);
        } catch (Exception e) {
            return ResponseData.error("获取客户端日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取统计信息
     */
    @Operation(summary = "获取统计信息", description = "获取系统的各项统计数据")
    @GetMapping("/statistics")
    public ResponseData getStatistics() {
        try {
            // 获取各种统计数据
            List<String> onlineClients = webSocketService.getOnlineClients();
            List<String> idleClients = webSocketService.getIdleClients();
            
            // 统计数据
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalClients", clientService.count());
            stats.put("onlineClients", onlineClients.size());
            stats.put("idleClients", idleClients.size());
            
            return ResponseData.success("统计信息", stats);
        } catch (Exception e) {
            return ResponseData.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有脚本
     */
    @Operation(summary = "获取所有脚本", description = "获取系统中所有爬虫脚本的信息")
    @GetMapping("/scripts")
    public ResponseData getAllScripts() {
        try {
            List<CrawlerScript> scripts = scriptService.list();
            return ResponseData.success("获取脚本列表成功", scripts);
        } catch (Exception e) {
            return ResponseData.error("获取脚本列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建新脚本
     */
    @Operation(summary = "创建新脚本", description = "创建一个新的爬虫脚本")
    @PostMapping("/scripts")
    public ResponseData createScript(@RequestBody CrawlerScript script) {
        try {
            scriptService.save(script);
            return ResponseData.success("脚本创建成功", script);
        } catch (Exception e) {
            return ResponseData.error("创建脚本失败: " + e.getMessage());
        }
    }

    /**
     * 更新脚本
     */
    @Operation(summary = "更新脚本", description = "根据脚本ID更新爬虫脚本信息")
    @PutMapping("/scripts/{scriptId}")
    public ResponseData updateScript(@Parameter(description = "脚本ID") @PathVariable String scriptId, @RequestBody CrawlerScript script) {
        try {
            script.setScriptId(scriptId);
            scriptService.updateById(script);
            return ResponseData.success("脚本更新成功", script);
        } catch (Exception e) {
            return ResponseData.error("更新脚本失败: " + e.getMessage());
        }
    }

    /**
     * 删除脚本
     */
    @Operation(summary = "删除脚本", description = "根据脚本ID删除爬虫脚本")
    @DeleteMapping("/scripts/{scriptId}")
    public ResponseData deleteScript(@Parameter(description = "脚本ID") @PathVariable String scriptId) {
        try {
            scriptService.removeById(scriptId);
            return ResponseData.success("脚本删除成功", scriptId);
        } catch (Exception e) {
            return ResponseData.error("删除脚本失败: " + e.getMessage());
        }
    }

    /**
     * 向指定客户端发送脚本
     */
    @Operation(summary = "向指定客户端发送脚本", description = "向指定客户端发送脚本并执行")
    @PostMapping("/clients/{clientId}/execute-script")
    public ResponseData executeScriptOnClient(@Parameter(description = "客户端ID") @PathVariable String clientId, @RequestBody Map<String, String> params) {
        try {
            String scriptId = params.get("scriptId");
            String scriptContent = params.get("scriptContent"); // 如果没有提供脚本内容，则从数据库获取
            
            if (scriptContent == null || scriptContent.isEmpty()) {
                CrawlerScript script = scriptService.getById(scriptId);
                if (script == null) {
                    return ResponseData.error("脚本不存在");
                }
                scriptContent = script.getContent();
            }
            
            // 发送脚本执行命令给客户端
            String message = String.format("{\"type\":\"execute_script\",\"scriptId\":\"%s\",\"scriptContent\":\"%s\"}", 
                                         scriptId, scriptContent.replace("\"", "\\\""));
            webSocketService.sendMessageToClient(clientId, message);
            
            return ResponseData.success("脚本已发送至客户端执行", clientId);
        } catch (Exception e) {
            return ResponseData.error("发送脚本到客户端失败: " + e.getMessage());
        }
    }
    
    /**
     * 指定客户端执行脚本库中的脚本
     */
    @Operation(summary = "指定客户端执行脚本库中的脚本", description = "指定客户端ID和脚本ID，执行脚本库中的脚本")
    @PostMapping("/clients/{clientId}/execute-script-by-id")
    public ResponseData executeScriptById(@Parameter(description = "客户端ID") @PathVariable String clientId, @RequestBody Map<String, String> params) {
        try {
            String scriptId = params.get("scriptId");
            
            // 验证客户端是否在线
            if (!webSocketService.isClientConnected(clientId)) {
                return ResponseData.error("指定的客户端不在线");
            }
            
            // 验证脚本是否存在
            CrawlerScript script = scriptService.getById(scriptId);
            if (script == null) {
                return ResponseData.error("脚本不存在");
            }
            
            // 通过WebSocket处理器向指定客户端发送执行脚本命令
            CrawlerWebSocketHandler handler = new CrawlerWebSocketHandler();
            handler.sendExecuteScriptCommand(clientId, "manual_task_" + System.currentTimeMillis(), scriptId, script.getContent());
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("clientId", clientId);
            result.put("scriptId", scriptId);
            result.put("scriptName", script.getDescription());
            
            return ResponseData.success("脚本已发送至指定客户端执行", result);
        } catch (Exception e) {
            return ResponseData.error("执行脚本失败: " + e.getMessage());
        }
    }
}