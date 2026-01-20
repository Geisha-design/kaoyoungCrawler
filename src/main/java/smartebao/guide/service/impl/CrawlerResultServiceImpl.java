package smartebao.guide.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerResult;
import smartebao.guide.mapper.CrawlerResultMapper;
import smartebao.guide.service.CrawlerResultService;

import java.util.Date;

@Service
public class CrawlerResultServiceImpl extends ServiceImpl<CrawlerResultMapper, CrawlerResult> implements CrawlerResultService {

    @Override
    public void saveCrawlResult(String resultId, String taskId, String clientId, String crawlData, String crawlStatus) {
        CrawlerResult result = new CrawlerResult();
        result.setResultId(resultId);
        result.setTaskId(taskId);
        result.setClientId(clientId);
        result.setCrawlData(crawlData);
        result.setCrawlStatus(crawlStatus);
        result.setCrawlTime(new Date());
        result.setStorageTime(new Date());
        
        save(result);
    }

    @Override
    public CrawlerResult getResultByTaskId(String taskId) {
        QueryWrapper<CrawlerResult> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId);
        return getOne(wrapper);
    }

    @Override
    public CrawlerResult getLatestResultByClientId(String clientId) {
        QueryWrapper<CrawlerResult> wrapper = new QueryWrapper<>();
        wrapper.eq("client_id", clientId)
               .orderByDesc("crawl_time")
               .last("LIMIT 1");
        return getOne(wrapper);
    }
}