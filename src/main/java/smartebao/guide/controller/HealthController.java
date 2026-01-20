package smartebao.guide.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.service.ClientHealthCheckService;
import smartebao.guide.utils.ResponseData;
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * 客户端健康检查控制器
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private ClientHealthCheckService healthCheckService;

    /**
     * 获取所有客户端健康状态
     */
    @GetMapping("/status")
    public ResponseData getAllClientHealthStatus() {
        try {
            // 获取所有客户端的健康状态
            List<Map<String, Object>> healthStatus = healthCheckService.getAllClientHealthStatus();
            return ResponseData.success("获取客户端健康状态成功", healthStatus);
        } catch (Exception e) {
            return ResponseData.error("获取客户端健康状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取不健康的客户端列表
     */
    @GetMapping("/unhealthy")
    public ResponseData getUnhealthyClients() {
        try {
            // 通过健康检查服务获取不健康的客户端
            List<Map<String, Object>> unhealthyClients = healthCheckService.getUnhealthyClients();
            return ResponseData.success("获取不健康客户端成功", unhealthyClients);
        } catch (Exception e) {
            return ResponseData.error("获取不健康客户端失败: " + e.getMessage());
        }
    }

    /**
     * 移除不健康的客户端
     */
    @DeleteMapping("/unhealthy/remove")
    public ResponseData removeUnhealthyClients() {
        try {
            // 通过健康检查服务移除不健康的客户端
            int removedCount = healthCheckService.removeUnhealthyClients();
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("removedCount", removedCount);
            return ResponseData.success("移除不健康客户端完成", result);
        } catch (Exception e) {
            return ResponseData.error("移除不健康客户端失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发健康检查
     */
    @PostMapping("/check")
    public ResponseData triggerHealthCheck() {
        try {
            // 触发健康检查服务进行检查
            healthCheckService.performHealthCheck();
            return ResponseData.success("健康检查已触发", null);
        } catch (Exception e) {
            return ResponseData.error("触发健康检查失败: " + e.getMessage());
        }
    }

    /**
     * 获取在线客户端数量
     */
    @GetMapping("/online-count")
    public ResponseData getOnlineClientCount() {
        try {
            int onlineCount = CrawlerWebSocketHandler.getOnlineCount();
            return ResponseData.success("获取在线客户端数量成功", onlineCount);
        } catch (Exception e) {
            return ResponseData.error("获取在线客户端数量失败: " + e.getMessage());
        }
    }
}