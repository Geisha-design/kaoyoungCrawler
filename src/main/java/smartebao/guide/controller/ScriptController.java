package smartebao.guide.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerScript;
import smartebao.guide.service.CrawlerScriptService;
import smartebao.guide.utils.ResponseData;
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.List;

@Tag(name = "爬虫脚本管理", description = "爬虫脚本增删改查及下发相关的API")
@RestController
@RequestMapping("/api/scripts")
public class ScriptController {

    @Autowired
    private CrawlerScriptService scriptService;

    @Autowired
    private CrawlerWebSocketHandler webSocketHandler;

    @Operation(summary = "获取所有脚本", description = "获取系统中所有爬虫脚本的列表")
    @GetMapping
    public ResponseData getAllScripts() {
        try {
            List<CrawlerScript> scripts = scriptService.list();
            return ResponseData.success("获取脚本列表成功", scripts);
        } catch (Exception e) {
            return ResponseData.error("获取脚本列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取脚本", description = "根据脚本ID获取特定的爬虫脚本信息")
    @GetMapping("/{scriptId}")
    public ResponseData getScriptById(@Parameter(description = "脚本ID") @PathVariable String scriptId) {
        try {
            CrawlerScript script = scriptService.getById(scriptId);
            if (script != null) {
                return ResponseData.success("获取脚本成功", script);
            } else {
                return ResponseData.error("脚本不存在");
            }
        } catch (Exception e) {
            return ResponseData.error("获取脚本失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建脚本", description = "创建一个新的爬虫脚本")
    @PostMapping
    public ResponseData createScript(@RequestBody CrawlerScript script) {
        try {
            // 生成唯一的scriptId
            script.setScriptId("script_" + System.currentTimeMillis());
            scriptService.save(script);
            return ResponseData.success("脚本创建成功", script);
        } catch (Exception e) {
            return ResponseData.error("脚本创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新脚本", description = "根据脚本ID更新爬虫脚本信息")
    @PutMapping("/{scriptId}")
    public ResponseData updateScript(@Parameter(description = "脚本ID") @PathVariable String scriptId, 
                                   @RequestBody CrawlerScript script) {
        try {
            script.setScriptId(scriptId);
            scriptService.updateById(script);
            return ResponseData.success("脚本更新成功", script);
        } catch (Exception e) {
            return ResponseData.error("脚本更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除脚本", description = "根据脚本ID删除爬虫脚本")
    @DeleteMapping("/{scriptId}")
    public ResponseData deleteScript(@Parameter(description = "脚本ID") @PathVariable String scriptId) {
        try {
            scriptService.removeById(scriptId);
            return ResponseData.success("脚本删除成功", null);
        } catch (Exception e) {
            return ResponseData.error("脚本删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量下发脚本到所有客户端", description = "将所有脚本批量下发到当前在线的所有客户端")
    @PostMapping("/push/batch")
    public ResponseData pushScriptsToAllClients() {
        try {
            // 获取所有脚本
            List<CrawlerScript> scripts = scriptService.list();
            if (scripts.isEmpty()) {
                return ResponseData.error("没有可用的脚本");
            }

            // 获取所有在线客户端并下发脚本
            List<String> onlineClients = CrawlerWebSocketHandler.getOnlineCount() > 0 ? 
                CrawlerWebSocketHandler.getSessionMap().keySet().stream().collect(java.util.stream.Collectors.toList()) : 
                java.util.Collections.emptyList();

            if (onlineClients.isEmpty()) {
                return ResponseData.error("当前没有在线客户端");
            }

            // 向每个在线客户端下发脚本
            for (String clientId : onlineClients) {
                webSocketHandler.sendScriptsToClient(clientId, "batch");
            }

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("scriptCount", scripts.size());
            result.put("clientCount", onlineClients.size());
            return ResponseData.success("脚本批量下发成功", result);
        } catch (Exception e) {
            return ResponseData.error("脚本批量下发失败: " + e.getMessage());
        }
    }
}