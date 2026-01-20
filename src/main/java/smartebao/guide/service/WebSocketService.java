package smartebao.guide.service;

import smartebao.guide.entity.CrawlerScript;
import smartebao.guide.utils.ResponseData;

import java.util.List;

public interface WebSocketService {
    void updateClientStatus(String clientId, String status);
    
    void saveCrawlResult(String taskId, String clientId, Object crawlData, String crawlStatus);
    
    List<CrawlerScript> getAllScripts();
    
    List<CrawlerScript> getScriptsByIds(List<String> scriptIds);
    
    void updateClientUrl(String clientId, String currentUrl);
    
    void updateClientIdleStatus(String clientId, Boolean idleStatus);
    
    void removeUnhealthyClient(String clientId);
    
    void updateClientHeartbeat(String clientId);
    
    boolean isClientHealthy(String clientId);
    
    void sendMessageToClient(String clientId, String message);

    void broadcastMessage(String message);

    List<String> getOnlineClients();

    void handleClientDisconnect(String clientId);

    void handleHeartbeat(String clientId);

    boolean isClientIdle(String clientId);

    void sendTaskToSpecificClient(String clientId, String taskType, String taskParams, Boolean executeOnIdle);

    void sendTaskToAllIdleClients(String taskType, String taskParams);

    boolean isClientConnected(String clientId);

    void setClientAttribute(String clientId, String key, Object value);

    Object getClientAttribute(String clientId, String key);

    List<String> getIdleClients();
    
    boolean sendTaskToIdleClients(String taskId, String taskType, String taskParams);
    
    ResponseData getAllClientStatus();
}