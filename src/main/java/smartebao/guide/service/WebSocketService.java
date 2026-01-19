package smartebao.guide.service;

import smartebao.guide.entity.CrawlerScript;

import java.util.List;

public interface WebSocketService {
    void updateClientStatus(String clientId, String status);
    
    void saveCrawlResult(String taskId, String clientId, Object crawlData, String crawlStatus);
    
    List<CrawlerScript> getAllScripts();
    
    List<CrawlerScript> getScriptsByIds(List<String> scriptIds);
    
    void updateClientUrl(String clientId, String currentUrl);
}