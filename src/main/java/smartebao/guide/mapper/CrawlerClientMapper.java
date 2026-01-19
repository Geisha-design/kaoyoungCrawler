package smartebao.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import smartebao.guide.entity.CrawlerClient;

@Mapper
public interface CrawlerClientMapper extends BaseMapper<CrawlerClient> {
}