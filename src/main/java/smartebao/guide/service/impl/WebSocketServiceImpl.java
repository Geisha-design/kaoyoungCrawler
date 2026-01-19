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
import smartebao.guide.service.WebSocketService;

import java.util.Date;
import java.util.List;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Autowired
    private CrawlerClientMapper crawlerClientMapper;

    @Autowired
    private CrawlerResultMapper crawlerResultMapper;

    @Autowired
    private CrawlerScriptMapper crawlerScriptMapper;

    @Override
    public void updateClientStatus(String clientId, String status) {
        QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
        wrapper.eq("client_id", clientId);
        CrawlerClient client = crawlerClientMapper.selectOne(wrapper);
        if (client != null) {
            client.setStatus(status);
            client.setLastUpdateTime(new Date());
            crawlerClientMapper.updateById(client);
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
        }
    }
}