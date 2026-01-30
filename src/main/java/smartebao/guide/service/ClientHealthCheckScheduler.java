//package smartebao.guide.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import smartebao.guide.utils.LogUtils;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * 客户端健康检查定时任务
// * 定期执行健康检查，清理不健康的客户端
// */
//@Service
//public class ClientHealthCheckScheduler {
//
//    @Autowired
//    private ClientHealthCheckService healthCheckService;
//
//    /**
//     * 定时执行健康检查，每60秒执行一次
//     * 根据客户端心跳时间判断客户端是否健康
//     * 对超过60秒未发送心跳的客户端，将其状态更新为离线
//     */
//    @Scheduled(fixedRate = 60000) // 每60秒执行一次
//    public void scheduledHealthCheck() {
//        LogUtils.logInfo("开始执行定时健康检查...");
//
//        try {
//            // 执行健康检查
//            healthCheckService.performHealthCheck();
//
//            // 获取不健康的客户端数量
//            List<Map<String, Object>> unhealthyClients = healthCheckService.getUnhealthyClients();
//            int unhealthyCount = unhealthyClients.size();
//
//            LogUtils.logInfo("定时健康检查完成，发现 " + unhealthyCount + " 个不健康的客户端");
//
//            if (unhealthyCount > 0) {
//                for (Map<String, Object> client : unhealthyClients) {
//                    LogUtils.logInfo("不健康客户端: " + client.get("clientId") + ", 最后心跳时间: " + client.get("lastHeartbeat"));
//                }
//            }
//        } catch (Exception e) {
//            LogUtils.logError("执行定时健康检查时发生异常", e);
//        }
//    }
//}