package smartebao.guide.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.entity.CrawlerScheduledTask;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.mapper.CrawlerScheduledTaskMapper;

import java.util.List;

@Service
public class ScheduledTaskService {

    @Autowired
    private CrawlerScheduledTaskMapper scheduledTaskMapper;

    @Autowired
    private CrawlerClientMapper clientMapper;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 每分钟检查一次定时任务
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void checkScheduledTasks() {
        System.out.println("开始检查定时任务...");

        // 查询所有启用的定时任务 - 使用自定义查询方法避免MySQL保留字问题
        List<CrawlerScheduledTask> tasks = scheduledTaskMapper.selectAllEnabledTasks();

        for (CrawlerScheduledTask task : tasks) {
            // 检查是否到了执行时间
            if (shouldExecuteTask(task)) {
                executeTask(task);
            }
        }

        System.out.println("定时任务检查完成");
    }

    /**
     * 判断是否应该执行任务
     */
    private boolean shouldExecuteTask(CrawlerScheduledTask task) {
        // 这里可以实现更复杂的调度逻辑
        // 比如检查上次执行时间、目标域名等
        // 简单实现：只要任务启用就按间隔执行
        return true;
    }

    /**
     * 执行定时任务
     */
    private void executeTask(CrawlerScheduledTask task) {
        try {
            // 检查目标客户端是否在线且健康
            if (webSocketService.isClientConnected(task.getClientId()) &&
                webSocketService.isClientHealthy(task.getClientId())) {

                // 获取客户端信息，检查其空闲状态
                QueryWrapper<CrawlerClient> clientQuery = new QueryWrapper<>();
                clientQuery.eq("client_id", task.getClientId());
                CrawlerClient client = clientMapper.selectOne(clientQuery);

                // 如果任务要求在空闲时执行，但客户端不空闲，则跳过
                if (task.getExecuteOnIdle() != null && task.getExecuteOnIdle() && 
                    client != null && client.getIdleStatus() != null && !client.getIdleStatus()) {
                    System.out.println("客户端 " + task.getClientId() + " 不在空闲状态，跳过定时任务 " + task.getTaskKey());
                    return;
                }

                // 生成任务ID
                String taskId = "scheduled_task_" + task.getTaskKey() + "_" + System.currentTimeMillis();

                // 发送任务指令到客户端
                webSocketService.sendTaskToSpecificClient(task.getClientId(), taskId, task.getScriptId(), task.getExecuteOnIdle());

                System.out.println("发送定时任务到客户端 " + task.getClientId() + ", 任务: " + task.getTaskKey());
            } else {
                System.out.println("客户端 " + task.getClientId() + " 不在线或不健康，跳过定时任务 " + task.getTaskKey());
            }
        } catch (Exception e) {
            System.err.println("执行定时任务时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}