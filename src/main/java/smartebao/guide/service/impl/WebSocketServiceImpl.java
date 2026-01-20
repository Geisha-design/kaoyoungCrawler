package smartebao.guide.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.entity.CrawlerResult;
import smartebao.guide.entity.CrawlerScript;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.mapper.CrawlerResultMapper;
import smartebao.guide.mapper.CrawlerScriptMapper;
import smartebao.guide.service.ClientCacheService;
import smartebao.guide.service.WebSocketService;
import smartebao.guide.utils.ResponseData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Autowired
    private CrawlerClientMapper crawlerClientMapper;

    @Autowired
    private CrawlerResultMapper crawlerResultMapper;

    @Autowired
    private CrawlerScriptMapper crawlerScriptMapper;

    @Autowired
    private ClientCacheService clientCacheService;

    // 存储WebSocket连接的映射关系
    private static final Map<String, Object> clientSessions = new ConcurrentHashMap<>();

    @Override
    public void updateClientStatus(String clientId, String status) {
        QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
        wrapper.eq("client_id", clientId);
        CrawlerClient client = crawlerClientMapper.selectOne(wrapper);
        if (client != null) {
            client.setStatus(status);
            client.setLastUpdateTime(new Date());
            crawlerClientMapper.updateById(client);
            
            // 更新缓存
            clientCacheService.setClientOnlineStatus(clientId, "online".equals(status));
            clientCacheService.cacheClientInfo(client);
        }
    }

    @Override
    public void updateClientIdleStatus(String clientId, Boolean idleStatus) {
        QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
        wrapper.eq("client_id", clientId);
        CrawlerClient client = crawlerClientMapper.selectOne(wrapper);
        if (client != null) {
            client.setIdleStatus(idleStatus);
            client.setLastUpdateTime(new Date());
            crawlerClientMapper.updateById(client);
            
            // 更新缓存
            clientCacheService.setClientIdleStatus(clientId, idleStatus);
            clientCacheService.cacheClientInfo(client);
        }
    }

    @Override
    public void saveCrawlResult(String taskId, String clientId, Object crawlData, String crawlStatus) {
        CrawlerResult result = new CrawlerResult();
        result.setResultId("result_" + System.currentTimeMillis()); // 生成唯一ID
        result.setTaskId(taskId);
        result.setClientId(clientId);
        result.setCrawlData(crawlData.toString()); // 实际应用中应该序列化为JSON字符串
        result.setCrawlStatus(crawlStatus);
        result.setCrawlTime(new Date());
        result.setStorageTime(new Date());
        crawlerResultMapper.insert(result);
    }

    @Override
    public List<CrawlerScript> getAllScripts() {
        return crawlerScriptMapper.selectList(null);
    }

    @Override
    public List<CrawlerScript> getScriptsByIds(List<String> scriptIds) {
        QueryWrapper<CrawlerScript> wrapper = new QueryWrapper<>();
        wrapper.in("script_id", scriptIds);
        return crawlerScriptMapper.selectList(wrapper);
    }

    @Override
    public void updateClientUrl(String clientId, String currentUrl) {
        QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
        wrapper.eq("client_id", clientId);
        CrawlerClient client = crawlerClientMapper.selectOne(wrapper);
        if (client != null) {
            client.setCurrentUrl(currentUrl);
            client.setLastUpdateTime(new Date());
            crawlerClientMapper.updateById(client);
            
            // 更新缓存
            clientCacheService.cacheClientInfo(client);
        }
    }

    @Override
    public void removeUnhealthyClient(String clientId) {
        // 更新客户端状态为offline
        updateClientStatus(clientId, "offline");
    }

    @Override
    public void updateClientHeartbeat(String clientId) {
        clientCacheService.updateClientHeartbeat(clientId);
    }

    @Override
    public boolean isClientHealthy(String clientId) {
        return clientCacheService.isClientHealthy(clientId, 60000); // 60秒超时
    }

    @Override
    public void sendMessageToClient(String clientId, String message) {
        // 实际的WebSocket消息发送逻辑应在WebSocket处理器中处理
        // 这里只是示例，实际实现需要访问WebSocket会话
        System.out.println("Sending message to client " + clientId + ": " + message);
    }

    @Override
    public void broadcastMessage(String message) {
        // 广播消息给所有连接的客户端
        System.out.println("Broadcasting message: " + message);
    }

    @Override
    public List<String> getOnlineClients() {
        List<CrawlerClient> clients = crawlerClientMapper.selectList(
            new QueryWrapper<CrawlerClient>().eq("status", "online")
        );
        List<String> onlineClientIds = new ArrayList<>();
        for (CrawlerClient client : clients) {
            onlineClientIds.add(client.getClientId());
        }
        return onlineClientIds;
    }

    @Override
    public void handleClientDisconnect(String clientId) {
        updateClientStatus(clientId, "offline");
        clientSessions.remove(clientId);
    }

    @Override
    public void handleHeartbeat(String clientId) {
        updateClientHeartbeat(clientId);
    }

    @Override
    public boolean isClientIdle(String clientId) {
        return clientCacheService.getClientIdleStatus(clientId);
    }

    @Override
    public void sendTaskToSpecificClient(String clientId, String taskType, String taskParams, Boolean executeOnIdle) {
        String taskMessage = String.format("{\"type\":\"task\", \"taskType\":\"%s\", \"taskParams\":\"%s\", \"executeOnIdle\":%s}", taskType, taskParams, executeOnIdle);
        sendMessageToClient(clientId, taskMessage);
    }

    @Override
    public void sendTaskToAllIdleClients(String taskType, String taskParams) {
        List<String> idleClients = getIdleClients();
        for (String clientId : idleClients) {
            sendTaskToSpecificClient(clientId, taskType, taskParams, true);
        }
    }

    @Override
    public boolean isClientConnected(String clientId) {
        return clientSessions.containsKey(clientId) && isClientHealthy(clientId);
    }

    @Override
    public void setClientAttribute(String clientId, String key, Object value) {
        // 可以在缓存中存储客户端属性
        clientCacheService.setString(clientId + ":" + key, value.toString());
    }

    @Override
    public Object getClientAttribute(String clientId, String key) {
        return clientCacheService.getString(clientId + ":" + key);
    }

    @Override
    public List<String> getIdleClients() {
        List<CrawlerClient> clients = crawlerClientMapper.selectList(
            new QueryWrapper<CrawlerClient>().eq("status", "online").eq("idle_status", true)
        );
        List<String> idleClientIds = new ArrayList<>();
        for (CrawlerClient client : clients) {
            idleClientIds.add(client.getClientId());
        }
        return idleClientIds;
    }

    @Override
    public boolean sendTaskToIdleClients(String taskId, String taskType, String taskParams) {
        List<String> idleClients = getIdleClients();
        if (!idleClients.isEmpty()) {
            // 发送到第一个空闲客户端
            String clientId = idleClients.get(0);
            sendTaskToSpecificClient(clientId, taskType, taskParams, true);
            return true;
        }
        return false; // 没有空闲客户端
    }

    @Override
    public ResponseData getAllClientStatus() {
        List<CrawlerClient> allClients = crawlerClientMapper.selectList(null);
        return ResponseData.success("获取客户端状态成功", allClients);
    }
}