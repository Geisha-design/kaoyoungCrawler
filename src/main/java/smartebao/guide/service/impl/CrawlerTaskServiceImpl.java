package smartebao.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerTask;
import smartebao.guide.mapper.CrawlerTaskMapper;
import smartebao.guide.service.CrawlerTaskService;

@Service
public class CrawlerTaskServiceImpl extends ServiceImpl<CrawlerTaskMapper, CrawlerTask> implements CrawlerTaskService {
}