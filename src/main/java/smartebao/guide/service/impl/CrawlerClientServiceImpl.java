package smartebao.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.service.CrawlerClientService;

@Service
public class CrawlerClientServiceImpl extends ServiceImpl<CrawlerClientMapper, CrawlerClient> implements CrawlerClientService {
}