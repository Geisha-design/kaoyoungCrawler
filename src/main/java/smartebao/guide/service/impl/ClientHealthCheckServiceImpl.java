package smartebao.guide.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.service.ClientCacheService;
import smartebao.guide.service.ClientHealthCheckService;
import smartebao.guide.service.WebSocketService;

import java.util.*;

@Service
public class ClientHealthCheckServiceImpl implements ClientHealthCheckService {

    @Autowired
    private CrawlerClientMapper crawlerClientMapper;

    @Autowired
    private ClientCacheService clientCacheService;

    @Autowired
    private WebSocketService webSocketService;

    @Override
    public List<Map<String, Object>> getAllClientHealthStatus() {
        List<CrawlerClient> clients = crawlerClientMapper.selectList(null);
        List<Map<String, Object>> healthStatusList = new ArrayList<>();

        for (CrawlerClient client : clients) {
            Map<String, Object> status = new HashMap<>();
            status.put("clientId", client.getClientId());
            status.put("username", client.getUsername());
            status.put("status", client.getStatus());
            status.put("idleStatus", client.getIdleStatus());
            status.put("currentUrl", client.getCurrentUrl());
            status.put("connectTime", client.getConnectTime());
            status.put("lastUpdateTime", client.getLastUpdateTime());
            status.put("isHealthy", webSocketService.isClientHealthy(client.getClientId()));

            healthStatusList.add(status);
        }

        return healthStatusList;
    }

    @Override
    public List<Map<String, Object>> getUnhealthyClients() {
        List<CrawlerClient> clients = crawlerClientMapper.selectList(null);
        List<Map<String, Object>> unhealthyClients = new ArrayList<>();

        for (CrawlerClient client : clients) {
            if (!webSocketService.isClientHealthy(client.getClientId())) {
                Map<String, Object> status = new HashMap<>();
                status.put("clientId", client.getClientId());
                status.put("username", client.getUsername());
                status.put("status", client.getStatus());
                status.put("lastHeartbeat", clientCacheService.getClientLastHeartbeat(client.getClientId()));
                status.put("isHealthy", false);

                unhealthyClients.add(status);
            }
        }

        return unhealthyClients;
    }

    @Override
    public int removeUnhealthyClients() {
        List<CrawlerClient> clients = crawlerClientMapper.selectList(null);
        int removedCount = 0;

        for (CrawlerClient client : clients) {
            if (!webSocketService.isClientHealthy(client.getClientId())) {
                // 更新客户端状态为offline
                webSocketService.updateClientStatus(client.getClientId(), "offline");
                removedCount++;
            }
        }

        return removedCount;
    }

    @Override
    public void performHealthCheck() {
        // 获取所有客户端
        List<CrawlerClient> clients = crawlerClientMapper.selectList(null);

        for (CrawlerClient client : clients) {
            // 检查客户端健康状况
            boolean isHealthy = webSocketService.isClientHealthy(client.getClientId());
            
            // 如果客户端不健康，更新其状态
            if (!isHealthy) {
                webSocketService.updateClientStatus(client.getClientId(), "offline");
            }
        }
    }
}