package smartebao.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerScript;
import smartebao.guide.mapper.CrawlerScriptMapper;
import smartebao.guide.service.CrawlerScriptService;

@Service
public class CrawlerScriptServiceImpl extends ServiceImpl<CrawlerScriptMapper, CrawlerScript> implements CrawlerScriptService {
}