package smartebao.guide.service;

import java.util.List;
import java.util.Map;

public interface ClientHealthCheckService {
    /**
     * 获取所有客户端健康状态
     */
    List<Map<String, Object>> getAllClientHealthStatus();

    /**
     * 获取不健康的客户端列表
     */
    List<Map<String, Object>> getUnhealthyClients();

    /**
     * 移除不健康的客户端
     */
    int removeUnhealthyClients();

    /**
     * 执行健康检查
     */
    void performHealthCheck();
}