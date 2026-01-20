package smartebao.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import smartebao.guide.entity.CrawlerResult;

public interface CrawlerResultService extends IService<CrawlerResult> {
    /**
     * 保存爬取结果
     */
    void saveCrawlResult(String resultId, String taskId, String clientId, String crawlData, String crawlStatus);
    
    /**
     * 根据任务ID获取结果
     */
    CrawlerResult getResultByTaskId(String taskId);
    
    /**
     * 根据客户端ID获取最近结果
     */
    CrawlerResult getLatestResultByClientId(String clientId);
}