package smartebao.guide.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartebao.guide.entity.CrawlerScheduledTask;
import smartebao.guide.mapper.CrawlerScheduledTaskMapper;

import java.util.*;

@RestController
@RequestMapping("/api/scheduled-task")
@Tag(name = "定时任务管理", description = "用于管理爬虫客户端定时任务的API")
public class ScheduledTaskController {

    @Autowired
    private CrawlerScheduledTaskMapper scheduledTaskMapper;

    @GetMapping("/list/{clientId}")
    @Operation(summary = "获取客户端定时任务列表", description = "根据客户端ID查询该客户端的所有定时任务")
    public ResponseEntity<Map<String, Object>> getScheduledTasks(@Parameter(description = "客户端ID", required = true) @PathVariable String clientId) {
        try {
            // 查询指定客户端的定时任务
            QueryWrapper<CrawlerScheduledTask> wrapper = new QueryWrapper<>();
            wrapper.eq("client_id", clientId);
            List<CrawlerScheduledTask> tasks = scheduledTaskMapper.selectList(wrapper);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", tasks);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/sync")
    @Operation(summary = "同步客户端定时任务", description = "将客户端的定时任务同步到服务器")
    public ResponseEntity<Map<String, Object>> syncScheduledTasks(@RequestBody Map<String, Object> request) {
        String clientId = (String) request.get("clientId");
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) request.get("tasks");

        try {
            // 先删除该客户端的所有现有定时任务
            QueryWrapper<CrawlerScheduledTask> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("client_id", clientId);
            scheduledTaskMapper.delete(deleteWrapper);

            // 插入新的定时任务
            for (Map<String, Object> task : tasks) {
                CrawlerScheduledTask scheduledTask = new CrawlerScheduledTask();
                scheduledTask.setTaskKey((String) task.get("taskKey"));
                scheduledTask.setClientId(clientId);
                scheduledTask.setUsername((String) task.get("username")); // 如果有提供的话
                scheduledTask.setScriptId((String) task.get("scriptId"));
                scheduledTask.setDomain((String) task.get("domain"));
                scheduledTask.setInterval(((Integer) task.get("interval")).longValue());
                scheduledTask.setEnabled((Boolean) task.get("enabled"));
                scheduledTask.setTaskName((String) task.get("taskName"));
                scheduledTask.setCreateTime(new Date());
                scheduledTask.setUpdateTime(new Date());
                
                scheduledTaskMapper.insert(scheduledTask);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "同步成功");
            response.put("data", null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "同步失败: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}