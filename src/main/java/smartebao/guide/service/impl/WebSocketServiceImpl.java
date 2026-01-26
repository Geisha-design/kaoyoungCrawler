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
import smartebao.guide.websocket.CrawlerWebSocketHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
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
        List<CrawlerClient> clients = crawlerClientMapper.selectList(wrapper);
        
        if (clients != null && !clients.isEmpty()) {
            // 使用最新的客户端记录（根据lastUpdateTime排序）
            CrawlerClient client = clients.stream()
                .max(Comparator.comparing(CrawlerClient::getLastUpdateTime))
                .orElse(clients.get(0)); // 如果没有lastUpdateTime字段值，则使用第一个
            
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
        wrapper.eq("status", "online");
        List<CrawlerClient> clients = crawlerClientMapper.selectList(wrapper);
        
        if (clients != null && !clients.isEmpty()) {
            // 使用最新的客户端记录（根据lastUpdateTime排序）
            CrawlerClient client = clients.stream()
                .max(Comparator.comparing(CrawlerClient::getLastUpdateTime))
                .orElse(clients.get(0)); // 如果没有lastUpdateTime字段值，则使用第一个
            
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
        wrapper.eq("status", "online");
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
        // 检查客户端ID是否为空
        if (clientId == null || clientId.trim().isEmpty()) {
            System.err.println("客户端ID为空，无法发送消息: " + message);
            return;
        }
        
        // 通过CrawlerWebSocketHandler获取客户端会话并发送消息
        javax.websocket.Session session = CrawlerWebSocketHandler.getClientSession(clientId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                System.out.println("成功发送消息到客户端 " + clientId + ": " + message);
            } catch (Exception e) {
                System.err.println("发送消息到客户端 " + clientId + " 失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("客户端 " + clientId + " 不在线或会话已关闭，无法发送消息: " + message);
            // 输出当前所有在线客户端的ID用于调试
            Map<String, javax.websocket.Session> allSessions = CrawlerWebSocketHandler.getSessionMap();
            if (allSessions.isEmpty()) {
                System.out.println("当前没有在线的客户端");
            } else {
                System.out.println("当前在线客户端列表: " + allSessions.keySet());
            }
        }
    }

    @Override
    public void broadcastMessage(String message) {
        // 从CrawlerWebSocketHandler获取所有会话并广播消息
        Map<String, javax.websocket.Session> sessions = CrawlerWebSocketHandler.getSessionMap();
        for (Map.Entry<String, javax.websocket.Session> entry : sessions.entrySet()) {
            String clientId = entry.getKey();
            javax.websocket.Session session = entry.getValue();
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    System.err.println("广播消息到客户端 " + clientId + " 失败: " + e.getMessage());
                }
            }
        }
        System.out.println("广播消息完成: " + message);
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
        // 检查WebSocket会话是否在线
        boolean isWebSocketConnected = CrawlerWebSocketHandler.isClientOnline(clientId);
        System.out.println("WebSocket连接状态: " + isWebSocketConnected);
        // 同时检查数据库状态
        boolean isDatabaseOnline = isClientOnlineInDatabase(clientId);
        System.out.println("数据库连接状态: " + isDatabaseOnline);

        // 只有当WebSocket连接和数据库状态都表明客户端在线时才认为客户端已连接
        return isWebSocketConnected && isDatabaseOnline;
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
    
    private boolean isClientOnlineInDatabase(String clientId) {
        QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
        wrapper.eq("client_id", clientId).eq("status", "online");
        return crawlerClientMapper.selectCount(wrapper) > 0;
    }
}