package smartebao.guide.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerTask;
import smartebao.guide.service.CrawlerTaskService;
import smartebao.guide.service.WebSocketService;
import smartebao.guide.service.ClientSelectionService;
import smartebao.guide.utils.ResponseData;
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * 外部系统任务控制器 - 供货代系统等外部系统调用
 */
@RestController
@RequestMapping("/api/external")
public class ExternalTaskController {

    @Autowired
    private CrawlerTaskService taskService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private ClientSelectionService clientSelectionService;

    /**
     * 外部系统发起任务请求
     * @param params 包含任务类型、参数等信息
     * @return 任务执行结果
     */
    @PostMapping("/task")
    public ResponseData executeTask(@RequestBody Map<String, Object> params) {
        try {
            // 创建任务
            CrawlerTask task = new CrawlerTask();
            task.setTaskType((String) params.get("taskType")); // 如 "booking_status_check"
            task.setTaskParams(params.get("taskParams").toString()); // 任务参数
            task.setTargetClients((String) params.get("targetClients")); // 目标客户端（可选）
            task.setPriority(1); // 默认优先级
            
            // 保存任务
            taskService.save(task);
            
            // 根据任务需求选择合适的客户端
            String taskType = (String) params.get("taskType");
            Map<String, Object> criteria = (Map<String, Object>) params.get("selectionCriteria");
            
            List<String> suitableClients;
            if (criteria != null && !criteria.isEmpty()) {
                // 根据条件筛选客户端
                suitableClients = clientSelectionService.selectClientsByTaskType(taskType, criteria);
            } else {
                // 默认选择空闲客户端
                suitableClients = clientSelectionService.getIdleClients();
            }
            
            if (!suitableClients.isEmpty()) {
                // 选择第一个符合条件的客户端执行任务
                String clientId = suitableClients.get(0);
                
                // 通过WebSocket向选定的客户端发送任务
                CrawlerWebSocketHandler handler = new CrawlerWebSocketHandler();
                handler.sendExecuteScriptCommand(clientId, task.getId().toString(), null, task.getTaskParams());
                
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("taskId", task.getId());
                result.put("clientId", clientId);
                return ResponseData.success("任务已发送至客户端执行", result);
            } else {
                return ResponseData.error("当前没有符合条件的可用客户端执行任务");
            }
        } catch (Exception e) {
            return ResponseData.error("任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务执行结果
     * @param taskId 任务ID
     * @return 任务结果
     */
    @GetMapping("/task/result/{taskId}")
    public ResponseData getTaskResult(@PathVariable String taskId) {
        try {
            // 这里可以实现具体的任务结果查询逻辑
            // 暂时返回模拟结果
            return ResponseData.success("正在获取任务结果", taskId);
        } catch (Exception e) {
            return ResponseData.error("获取任务结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端状态
     * @return 客户端列表及其状态
     */
    @GetMapping("/clients/status")
    public ResponseData getClientStatus() {
        try {
            // 返回所有客户端的状态信息
            return webSocketService.getAllClientStatus();
        } catch (Exception e) {
            return ResponseData.error("获取客户端状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 指定特定客户端执行任务
     * @param clientId 客户端ID
     * @param params 任务参数
     * @return 执行结果
     */
    @PostMapping("/task/client/{clientId}")
    public ResponseData executeTaskOnSpecificClient(@PathVariable String clientId, @RequestBody Map<String, Object> params) {
        try {
            // 创建任务
            CrawlerTask task = new CrawlerTask();
            task.setTaskType((String) params.get("taskType"));
            task.setTaskParams((String) params.get("taskParams"));
            task.setPriority(1);
            
            // 保存任务
            taskService.save(task);
            
            // 检查指定客户端是否存在且在线
            List<String> availableClients = clientSelectionService.getClientsByIds(java.util.Arrays.asList(clientId));
            if (availableClients.contains(clientId)) {
                // 向指定客户端发送任务
                CrawlerWebSocketHandler handler = new CrawlerWebSocketHandler();
                handler.sendExecuteScriptCommand(clientId, task.getId().toString(), null, task.getTaskParams());
                
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("taskId", task.getId());
                result.put("clientId", clientId);
                return ResponseData.success("任务已发送至指定客户端执行", result);
            } else {
                return ResponseData.error("指定的客户端不存在或不在线");
            }
        } catch (Exception e) {
            return ResponseData.error("任务执行失败: " + e.getMessage());
        }
    }
}